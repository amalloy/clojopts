(ns clojopts.getopt
  (:use clojopts.util)
  (:import (gnu.getopt Getopt LongOpt)))

(defn read-opt [{:keys [gnu-obj long-opts]}]
  (let [opt-int (.getopt gnu-obj)]
    (when-not (= -1 opt-int)
      (let [opt-name (if-not (zero? opt-int)
                       (str (char opt-int))
                       (.getName (nth long-opts
                                      (.getLongind gnu-obj))))
            opt-val (.getOptarg gnu-obj)]
        [opt-name opt-val]))))

(defn get-remaining-args [{:keys [gnu-obj argv]}]
  {:pre [(= -1 (.getopt gnu-obj))]}
  (drop (.getOptind gnu-obj) argv))

(defn make-getopt
  ([prog-name opt-string argv]
     (make-getopt prog-name opt-string [] argv))
  ([prog-name opt-string long-opts argv]
     (let [argv-array (into-array String (seq argv))
           longopt-array (into-array LongOpt (seq long-opts))]
       {:argv argv-array
        :longopt-array longopt-array
        :gnu-obj (Getopt. prog-name
                          argv-array
                          opt-string
                          longopt-array)})))

(defn getopt-seq
  "Turn iterative calls to gnu getopt into a lazy seq of return values.

=> (getopt-seq nil \"ab:c::d\" (into-array String [\"-ab\" \"10\"]))
     ([\\a nil] [\\b \"10\"])"
  [getopt]
  [(doall ; make sure getopt side effects happen first
    (take-while identity
                (repeatedly #(read-opt getopt))))
   (get-remaining-args getopt)])

(defn getopt-map
  "Turn an option seq into a map of keyword=>[values] pairs. If 

=> (getopt-map (getopt-seq nil \"ab:c::d\" (into-array String [\"-ab\" \"10\"])))
 {:b [\"10\"], :a [nil]}
=> (getopt-map (getopt-seq nil \"ab:c::d\" (into-array String [\"-ab\" \"10\" \"-b\" \"1\"])))
 {:b [\"10\" \"1\"], :a [nil]}"
  [[opt-seq non-opts]]
  (assoc
      (reduce
       (fn [m [opt arg]]
         (update-in m [opt]
                    (fnil conj []) arg))
       {} opt-seq)
    :clojopts/more non-opts))
