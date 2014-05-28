(ns clojopts.test.core
  (:use clojopts.core
        clojure.test)
  (:require [clojopts.ui :as ui]))

(deftest integration
  (letfn [(parse []
            (clojopts "clojopts"
                      ["--name=src/clojure/awesome.clj" "y" "-n" "10" "--" "-foo" "x"]
                      (optional-arg file f name "The file to use"
                                    :parse #(java.io.File. %))
                      (with-arg lines n "How many lines to read")))]
    (is (= {:file (java.io.File. "src/clojure/awesome.clj") :lines 10
            :clojopts/more ["y" "-foo" "x"]}
           (parse)))
    (is (= {:file (java.io.File. "src/clojure/awesome.clj")
            :clojopts/more ["y" "-n" "10" "--" "-foo" "x"]}
           (binding [ui/*stop-at-first-non-option* true]
             (parse))))))
