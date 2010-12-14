(ns cljopts.util)

(def ! complement)

(defn verify [pred x]
  (when (pred x)
    x))

(defn validator [pred]
  (fn [x]
    (verify pred x)))

(defmacro keywordize
  "Create a map in which, for each symbol S in vars, (keyword S) is a
  key mapping to the value of S in the current scope."
  [vars]
  (into {} (map (juxt keyword identity)
                vars)))

(defn transform-if
  "Tests pred against its argument. If the result is logical true,
return (f arg); otherwise, return (f-not arg) (defaults to identity)."
  ([pred f x]
     (if (pred x) (f x) x))
  ([pred f f-not x]
     (if (pred x) (f x) (f-not x))))