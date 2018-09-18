(ns tests
  (:require [clojure.test :as test :refer [deftest testing is]])
  (:import com.github.lxbr.calc.PostfixCalc
           java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream))

(deftest ops
  (is (== 42 (PostfixCalc/add 41 1)))
  (is (== 42 (PostfixCalc/subtract 43 1)))
  (is (== 42 (PostfixCalc/multiply 6 7)))
  (is (== 42 (PostfixCalc/divide 126 3))))

(deftest tokenization
  (is (= ["41" "1" "+"] (PostfixCalc/tokenize "41 1 +"))))

(deftest parsing
  (is (= [{"token" "41"
           "kind" "num"}
          {"token" "1"
           "kind" "num"}
          {"token" "+"
           "kind" "add"}
          {"token" "-"
           "kind" "sub"}
          {"token" "*"
           "kind" "mul"}
          {"token" "/"
           "kind" "div"}]
         (-> (PostfixCalc/tokenize "41 1 + - * /")
             (PostfixCalc/parse)))))

(deftest interpretation
  (is (== 42 (-> "41 1 +"
                 (PostfixCalc/tokenize)
                 (PostfixCalc/parse)
                 (PostfixCalc/interpret))))
  (is (== 42 (-> "43 1 -"
                 (PostfixCalc/tokenize)
                 (PostfixCalc/parse)
                 (PostfixCalc/interpret))))
  (is (== 42 (-> "6 7 *"
                 (PostfixCalc/tokenize)
                 (PostfixCalc/parse)
                 (PostfixCalc/interpret))))
  (is (== 42 (-> "126 3 /"
                 (PostfixCalc/tokenize)
                 (PostfixCalc/parse)
                 (PostfixCalc/interpret))))
  (is (== 42 (-> "6 7 * 1 + 3 * 3 - 3 /"
                 (PostfixCalc/tokenize)
                 (PostfixCalc/parse)
                 (PostfixCalc/interpret)))))

(test/run-tests)


