(ns cljopts.getopt
  (:use cljopts.util)
  (:import (gnu.getopt Getopt LongOpt)))

(defn read-opt [gnu-obj]
  [(verify (! #{-1})
           (.getopt gnu-obj))
   (.getOptarg gnu-obj)])

(defn getopt-seq
  "Turn iterative calls to gnu getopt into a lazy seq of return valuse.

=> (getopt-seq nil \"ab:c::d\" (into-array String [\"-ab\" \"10\"]))
     ([\\a nil] [\\b \"10\"])"
  [prog-name opt-string argv]
  (let [getopt (Getopt. prog-name argv opt-string)]
    (map (fn [[opt arg]]
           [(-> opt char str keyword)
            arg])
         (take-while first
                     (repeatedly #(read-opt getopt))))))

(defn getopt-map
  "Turn an option seq into a map of option=>value pairs. If 

=> (getopt-map (getopt-seq \"getopt\" \"ab:c::d\" (into-array String [\"-ab\" \"10\"])))
{:b \"10\", :a nil}
=> (getopt-map (getopt-seq \"getopt\" \"ab:c::d\" (into-array String [\"-ab\" \"10\" \"-b\" \"1\"])))
{:b [\"10\" \"1\"], :a nil}"
  [opt-seq]
  (reduce
   (fn [m [opt arg]]
     (update-in m [opt]
                (fn [v]
                  (cond
                   (nil? v) arg
                   (vector? v) (conj v arg)
                   :else [v arg]))))
   {} opt-seq)) 