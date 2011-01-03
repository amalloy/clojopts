(ns clojopts.parse
  (:use clojopts.getopt)
  (:import java.io.File))

(def boolean-parser (complement #{"no" "false" "0"}))

(defn auto-type [val]
  (cond
   (not val) true ; no argument, so probably just a boolean/toggle/flag
   (sequential? val) val           ; some kind of list; leave it alone
   :else
   (try
     (Integer. val)
     (catch Exception _
       (try
         (Double. val)
         (catch Exception _
           (let [f (File. val)]
             (if (.exists f)
               f
               (if-not (boolean-parser val)
                 false
                 val)))))))))

(def types
     {:int #(Integer. %)
      :boolean boolean-parser
      :file #(File. %)
      :str identity
      :guess auto-type})

(defn groupfn [f]
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

(defn maybe-parse
  "A poor man's maybe-m: apply the supplied function to anything but
  nil."
  ([f]
     (fn [x]
       (when-not (nil? x)
         (f x)))))

(defn parse-fn
  "Build a function for parsing an option-list, with the specified
grouping strategy, type coercion strategy, user-specified
parser/transformer, and default value. All of these arguments are
optional."
  ([{:keys [type group parse default]
     :or {type :guess, group :maybe-map, parse identity}}]
     (let [group-fn (grouping group)
           type (types type)]
       (fn [args]
         (or (group-fn (apply comp (map maybe-parse [parse type]))
                       (seq args))
             default)))))

