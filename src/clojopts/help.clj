(ns clojopts.help
  (:use (clojopts getopt util))
  (:require [clojure.string :as s]))

(def ^{:dynamic true} *margin* 2)
(def ^{:dynamic true} *page-width* 80)

(defmulti name-of (fn [name arg-type switch]
                    (count switch)))

(defmethod name-of 1 [name arg-type switch]
  (str "-" switch
       (case arg-type
             :optional (str "[" name "]")
             :required (str " " name)
             "")))

(defmethod name-of :default [name arg-type switch]
  (str "--" switch
       (case arg-type
             :optional (str "[=" name "]")
             :required (str "=" name)
             "")))

(defn name-column [spec]
  (let [{:keys [user-name arg]} spec]
    (mapcat #(map (partial name-of user-name arg) %)
            ((juxt :short-names :long-names) spec))))

(defn name-col-width [name-cols]
  (reduce max (mapcat #(map count %) name-cols)))

(defn padded-lines [num-lines format-fn line-src]
  (->> (repeat "")
       (concat line-src)
       (take num-lines)
       (map format-fn)))

(defn help-line [name-width spec]
  (let [name-width (+ *margin* name-width)
        [labels doc] ((juxt name-column :doc) spec)
        [doc lines] ((juxt identity count)
                     (word-wrap doc (- *page-width* name-width)))
        lines (max lines (count labels))]
    (apply str
           (apply interleave
                  (map (comp (partial apply padded-lines lines))
                       [[#(format (str "%" name-width "s")
                                  (apply str % (repeat *margin* " ")))
                         labels]
                        [#(str % \newline)
                         doc]])))))

(defn print-help [specs]
  (let [name-width (name-col-width (map name-column specs))]
    (print (s/join \newline (map (partial help-line name-width) specs)))))