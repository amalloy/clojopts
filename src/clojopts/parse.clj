(ns clojopts.parse
  (:use clojopts.getopt))

(def types
     {:int #(Integer. %)
      :boolean (complement #{"no" "false"})
      :file #(java.io.File. %)
      :str identity})

(defn- groupfn [f]
  (fn [parse args]
    (when (seq args)
      (parse (f args)))))

(def grouping
     {:last (groupfn last)
      :first (groupfn first)
      :list #(when %2 (%1 %2))
      :map #(when (seq %2) (map %1 %2))
      :maybe-list #(when (seq %2)
                     (if (> (count %2) 1)
                       (map %1 %2)
                       (%1 (first %2))))})

;;(group (or parse type) argv)
