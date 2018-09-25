(ns com.github.lxbr.jil
  (:refer-clojure :exclude [load-file])
  (:require [clojure.java.io :as io]
            [clojure.pprint])
  (:import java.io.ByteArrayOutputStream
           org.objectweb.asm.ClassReader
           java.io.File
           (javax.tools DiagnosticCollector
                        ForwardingJavaFileManager
                        JavaFileObject$Kind
                        SimpleJavaFileObject
                        ToolProvider)))

(set! *warn-on-reflection* true)

(defn class-cache
  []
  (-> (.getDeclaredField clojure.lang.DynamicClassLoader "classCache")
      (doto (.setAccessible true))
      (.get nil)))

(defn class-object
  [class-name baos]
  (let [file-name (.replace ^String class-name "." "/")
        extension (.extension JavaFileObject$Kind/CLASS)
        uri-string (str "string:///" file-name extension)
        uri (java.net.URI/create uri-string)]
    (proxy [SimpleJavaFileObject] [uri JavaFileObject$Kind/CLASS]
      (openOutputStream [] baos))))

(defn source-output-object
  [class-name]
  (let [baos (ByteArrayOutputStream.)]
    (proxy [SimpleJavaFileObject]
        [(java.net.URI/create (str "string:///"
                                   (.replace ^String class-name "." "/")
                                   (. JavaFileObject$Kind/SOURCE extension)))
         JavaFileObject$Kind/SOURCE]
      (getCharContent [_]
        (String. (.toByteArray baos)))
      (openOutputStream []
        baos))))

(defn class-manager
  [file-manager cache]
  (proxy [ForwardingJavaFileManager] [file-manager]
    (getJavaFileForOutput [location class-name kind sibling]
      (if (= kind JavaFileObject$Kind/SOURCE)
        (source-output-object class-name)
        (let [baos (ByteArrayOutputStream.)]
          (swap! cache assoc class-name baos)
          (class-object class-name baos))))))

(defn source-object
  [data]
  (let [{:keys [source]
         class-name :name} data
        file-name (.replace ^String class-name "." "/")
        extension (.extension JavaFileObject$Kind/SOURCE)
        uri-string (str "string:///" file-name extension)
        uri (java.net.URI/create uri-string)]
    (proxy [SimpleJavaFileObject] [uri JavaFileObject$Kind/SOURCE]
      (getCharContent [_] source))))

(defn source-file-object
  [^java.io.File file]
  (let [uri (.toURI file)]
    (proxy [SimpleJavaFileObject] [uri JavaFileObject$Kind/SOURCE]
      (getCharContent [_] (slurp file)))))

(defn print-diagnostics
  [^DiagnosticCollector diag]
  (doseq [^javax.tools.Diagnostic
          d (.getDiagnostics diag)]
    (let [k (.getKind d)]
      (clojure.pprint/pprint
       (if (nil? (.getSource d))
         {:kind (.toString k)
          :message (.getMessage d nil)}
         {:kind (.toString k)
          :source (.getName ^javax.tools.FileObject (.getSource d))
          :line (.getLineNumber d)
          :message (.getMessage d nil)})))))

(defn make-graph
  [bytes-by-class-name]
  (let [class-names (set (keys bytes-by-class-name))
        graph (->> (vals bytes-by-class-name)
                   (map (fn [^bytes bytes]
                          (let [class-reader (ClassReader. bytes)
                                parents (into #{} (comp (map #(.replace ^String % "/" "."))
                                                        (filter #(contains? class-names %)))
                                              (concat
                                               (.getInterfaces class-reader)
                                               [(.getSuperName class-reader)]))
                                class-node (org.objectweb.asm.tree.ClassNode.)]
                            (.accept class-reader class-node 0)
                            {:name (-> (.getClassName class-reader)
                                       (.replace "/" "."))
                             :source (.-sourceFile class-node)
                             :parents parents})))
                   (zipmap (keys bytes-by-class-name)))]
    (->> (for [{:keys [name parents]} (vals graph)
               parent parents]
           [parent name])
         (reduce
          (fn [graph [parent child]]
            (update-in graph [parent :children] (fnil conj #{}) child))
          graph))))

(defn sort-types
  [graph]
  (->> {:roots []
        :graph graph
        :sorted []}
       (iterate
        (fn [{:keys [roots graph sorted]}]
          (if-some [{:keys [name children]} (first roots)]
            {:roots (next roots)
             :graph (reduce
                     (fn [graph child]
                       (update-in graph [child :parents] disj name))
                     (dissoc graph name)
                     children)
             :sorted (conj sorted name)}
            {:roots (filter #(empty? (:parents %)) (vals graph))
             :graph graph
             :sorted sorted})))
       (drop-while (comp pos? count :graph))
       (first)
       (:sorted)))

(defn class-loader
  ^clojure.lang.DynamicClassLoader []
  (clojure.lang.RT/makeClassLoader))

(defn write-class-file
  [^String class-name bytes]
  (let [compile-path (or *compile-path* "classes")
        file-name (str compile-path File/separator (.replace class-name "." "/") ".class")
        class-file (io/file file-name)]
    (io/make-parents class-file)
    (io/copy bytes class-file)))

(defn load-classes
  [baos-by-class-name]
  (let [bytes-by-class-name (->> (for [[class-name ^ByteArrayOutputStream baos]
                                       baos-by-class-name]
                                   [class-name (.toByteArray baos)])
                                 (into {}))
        graph (make-graph bytes-by-class-name)
        ^java.util.Map
        cache (class-cache)
        cl (class-loader)]
    (doseq [name (sort-types graph)
            :let [{:keys [source]} (get graph name)
                  bytes (.toByteArray ^ByteArrayOutputStream (get baos-by-class-name name))
                  _ (write-class-file name bytes)
                  class-sym (symbol (last (.split ^String name "\\.")))]]
      (.remove cache name)
      (let [class (.defineClass cl name bytes nil)]
        (doseq [ns (all-ns)
                :let [^Class cls (get (ns-map ns) class-sym)]
                :when (and (some? cls) (= (.getCanonicalName cls) name))]
          (.importClass ^clojure.lang.Namespace ns class))))))

(defn load-files
  [files]
  (let [diag (DiagnosticCollector.)
        compiler (ToolProvider/getSystemJavaCompiler)
        file-manager (.getStandardFileManager compiler nil nil nil)
        cache (atom {})
        task (->> (mapv (comp source-file-object io/file) files)
                  (.getTask compiler nil (class-manager file-manager cache) diag nil nil))
        ret (.call task)]
    (print-diagnostics diag)
    (when (true? ret)
      (load-classes @cache))))

(defn load-file
  [path]
  (load-files [path]))

(defn load-dir
  [path]
  (->> (file-seq (io/file path))
       (filter (fn [^java.io.File file]
                 (.endsWith (.getName file) ".java")))
       (load-files)))
