(ns cljopts.util)

(def ! complement)

(defn verify [pred x]
  (when (pred x)
    x))

(defn validator [pred]
  (fn [x]
    (verify pred x)))