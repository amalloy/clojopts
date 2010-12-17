(ns cljopts.getopt
  (:use cljopts.util)
  (:import (gnu.getopt Getopt LongOpt)))

(defn read-opt [gnu-obj long-opts]
  (let [opt-int (.getopt gnu-obj)]
    (when-not (= -1 opt-int)
      (let [opt-name (if-not (zero? opt-int)
                       (str (char opt-int))
                       (.getName (nth long-opts
                                      (.getLongind gnu-obj))))
            opt-val (.getOptarg gnu-obj)]
        [opt-name opt-val]))))

(defn make-getopt
  ([prog-name opt-string argv]
     (make-getopt prog-name opt-string [] argv))
  ([prog-name opt-string long-opts argv]
     (Getopt. prog-name
              (into-array String (seq argv))
              opt-string
              (into-array LongOpt (seq long-opts)))))

(defn getopt-seq
  "Turn iterative calls to gnu getopt into a lazy seq of return values.

=> (getopt-seq nil \"ab:c::d\" (into-array String [\"-ab\" \"10\"]))
     ([\\a nil] [\\b \"10\"])"
  [getopt long-opts]
  (take-while identity
              (repeatedly #(read-opt getopt long-opts))))

(defn getopt-map
  "Turn an option seq into a map of keyword=>[values] pairs. If 

=> (getopt-map (getopt-seq nil \"ab:c::d\" (into-array String [\"-ab\" \"10\"])))
{:b [\"10\"], :a [nil]}
=> (getopt-map (getopt-seq nil \"ab:c::d\" (into-array String [\"-ab\" \"10\" \"-b\" \"1\"])))
{:b [\"10\" \"1\"], :a [nil]}"
  [opt-seq]
  (reduce
   (fn [m [opt arg]]
     (update-in m [opt]
                (fnil conj []) arg))
   {} opt-seq))

