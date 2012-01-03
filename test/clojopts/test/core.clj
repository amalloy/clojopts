(ns clojopts.test.core
  (:use clojopts.core)
  (:use clojure.test))

(deftest integration
  (is (= (clojopts "clojopts"
                   ["--name=src/clojure/awesome.clj" "-n" "10"]
                   (optional-arg file f name "The file to use"
                                 :parse #(java.io.File. %))
                   (with-arg lines n "How many lines to read"))
         {:file (java.io.File. "src/clojure/awesome.clj") :lines 10})))
