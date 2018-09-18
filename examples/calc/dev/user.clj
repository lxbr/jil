(ns user
  (:require [clojure.main]
            [com.github.lxbr.jil :as jil]
            [compliment.core])
  (:import com.github.lxbr.calc.PostfixCalc))

(comment

  ;; 10 LOAD   Java source code
  ;; 20 EVAL   Java source code
  ;; 30 CHANGE Java source code
  ;; GOTO 10
  (jil/load-dir "src")
  
  (PostfixCalc/eval "1 2 +")

  )

(defn shell-init
  []
  (println "Exit the shell by entering `quit`"))

(defn shell-prompt
  []
  (print "calc=> "))

(defn shell-read
  [_ quit]
  (let [line (.readLine *in*)]
    (if (= "quit" line)
      quit
      line)))

(defn shell-eval
  [data]
  (try
    (PostfixCalc/eval data)
    (catch Throwable t
      t)))

(defn shell-print
  [arg]
  (if (instance? Throwable arg)
    (println "invalid input")
    (prn arg)))

(defn shell
  []
  (clojure.main/repl
   :init   shell-init
   :prompt shell-prompt
   :read   shell-read
   :eval   shell-eval
   :print  shell-print))
