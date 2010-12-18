(ns clojopts.parse
  (:use clojopts.getopt)
  (:import java.io.File))

(declare types)

(defn auto-type [val]
  (if-not val
    true ; no argument, so probably just a boolean/toggle/flag
    (try
      (Integer. val)
      (catch Exception _
        (try
          (Double. val)
          (catch Exception _
            (let [f (File. val)]
              (if (.exists f)
                f
                (if-not ((types :boolean) val)
                  false
                  val)))))))))

(def types
     {:int #(Integer. %)
      :boolean (complement #{"no" "false" "0"})
      :file #(File. %)
      :str identity
      :guess auto-type})

(defn- groupfn [f]
  (fn [parse args]
    (when (seq args)
      (parse (f args)))))

;; Each value in this map is a function of two arguments: first a
;; parsing function to be applied to the elements of argv, and second
;; argv itself. The idea is that grouping specifies how to break up
;; multiple arguments before applying the parse function to them.
(def grouping
  {:last (groupfn last)               ; ignore all but the last 
   :first (groupfn first)             ; ignore all but the first
   :list #(when %2 (%1 %2))           ; parse the list as a whole
   :map #(when (seq %2) (map %1 %2))  ; parse each element of the list
   ;; same as map, unless there is exactly one element, in which case
   ;; don't wrap it in a list
   :maybe-map #(when (seq %2) 
                 (if (> (count %2) 1)
                   (map %1 %2)
                   (%1 (first %2))))})
