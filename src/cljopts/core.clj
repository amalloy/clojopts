(ns cljopts.core
  (:use cljopts.getopt
        [cljopts.util :only [keywordize and-print]]
        [clojure.contrib.seq :only [separate]])
  (:require [cljopts.parse :as parse])
  (:import (gnu.getopt Getopt LongOpt)))

(defn- parse-fn [{:keys [type group parse default]
                  :or {type :str, group :maybe-list, parse identity}}]
  (let [group-fn (parse/grouping group)
        type (parse/types type)]
    (fn [args]
      (if-not (seq args)
        default
        (or (group-fn (comp parse type) args)
            default)))))

(defn- option [names doc & specs]
  (let [[name names] ((apply juxt (if (vector? names)
                                    [first identity]
                                    [identity vector]))
                       names),
        {:keys [arg default user-name id]
         :or {arg :none, user-name name, id (keyword name)}
         :as specs}
        specs,
        parse (parse-fn specs)
        [short-names long-names] (separate #(= (.length %) 1) names)]
    (keywordize [name names short-names
                 long-names arg
                 parse user-name
                 default id])))

(defmacro arg-type [fname arg-val]
  `(defn ~fname {:arglists '~'([name doc & specs])}
     ([name# doc# & specs#]
        (apply option name# doc# (list* :arg ~arg-val specs#)))))

(arg-type no-arg :none)
(arg-type with-arg :required)
(arg-type optional-arg :optional)

(def opt-list hash-set)

(def long-opt-argmode {:none LongOpt/NO_ARGUMENT
                       :required LongOpt/REQUIRED_ARGUMENT
                       :optional LongOpt/OPTIONAL_ARGUMENT})

(defn build-getopt-fragment [spec]
  (let [suffix (case (:arg spec)
                     :none ""
                     :required ":"
                     :optional "::")
        {names :short-names} spec]
    (apply str (map str names (repeat suffix)))))

(defn get-long-opts [spec]
  (let [{:keys [short-names long-names arg]} spec
        short (first short-names)
        arg-arg (long-opt-argmode arg)
        [buf alias] (if short
                      [nil (int (first short))]
                      [(StringBuffer.) 0])]
    (map #(LongOpt. % arg-arg buf alias) long-names)))

(defn parse-cmdline-from-specs [specs argv & [prog-name]]
  (let [long-opts (mapcat get-long-opts specs)]
    (getopt-map
     (getopt-seq
      (make-getopt prog-name
                   (apply str (mapcat build-getopt-fragment specs))
                   long-opts
                   argv)
      long-opts))))

(defn merge-opt-map [specs getopt-map]
  (into {} (for [{:keys [id names parse] :as spec} specs]
             (when-let [args (seq (filter (comp 
                                           (set names)
                                           key)
                                          getopt-map))]
               {id (reduce into (map (comp parse val) 
                                     args))}))))

(comment Sample usage
         (cljopts "cljopts"
                  ["-v" "--with-name" "cljopts"]
                  (with-arg ["n" "with-name"] "The name to use" :parse first)
                  (no-arg "v" "Verbose mode" :id :verbose :parse boolean)))

(defn cljopts*
  ([prog-name argv & specs]
     (merge-opt-map specs
                    (parse-cmdline-from-specs specs argv prog-name))))

(defn desugar-spec [spec]
  (let [[type & more] spec
        [names [doc & opts]] (split-with (complement string?) more)]
    `(~type ~(vec (map str names)) ~doc ~@opts)))

(defmacro cljopts [prog-name argv & specs]
  `(cljopts* ~prog-name ~argv ~@(vec (map desugar-spec specs))))
