(ns clojopts.core
  (:use clojopts.getopt
        clojopts.util
        [clojure.contrib.seq :only [separate]])
  (:require [clojopts.parse :as parse])
  (:import (gnu.getopt Getopt LongOpt)))

(defn- maybe-parse
  "A poor man's maybe-m: apply the supplied function to anything but
  nil."
  ([f]
     (fn [x]
       (when-not (nil? x)
         (f x)))))

(defn- parse-fn
  "Build a function for parsing an option-list, with the specified
grouping strategy, type coercion strategy, user-specified
parser/transformer, and default value. All of these arguments are
optional."
  ([{:keys [type group parse default]
     :or {type :guess, group :maybe-map, parse identity}}]
     (let [group-fn (parse/grouping group)
           type (parse/types type)]
       (fn [args]
         (or (group-fn (apply comp (map maybe-parse [parse type]))
                       (seq args))
             default)))))

(defn- option
  "Takes a name (or vector of names), a docstring, and an optional set
of :option value pairs, and returns an attribute map representing all
that information in a single (internal to clojopts) object."
  ([names doc & specs]
     (let [[name names] ((apply juxt (if (vector? names)
                                       [first identity]
                                       [identity vector]))
                         names),
           {:keys [arg default id]
            :or {arg :none, id (keyword name)}}
           specs,
           parse (parse-fn specs)
           [short-names long-names] (separate #(= (.length %) 1) names)]
       (keywordize [name names short-names
                    long-names arg
                    parse default id]))))

(defmacro arg-type
  "Template for building versions of option (see above) with different
argument-required-ness parameters."
  ([fname arg-val]
     `(defn ~fname {:arglists '~'([name(s) doc & specs])}
        ([names# doc# & specs#]
           (apply option names# doc# (list* :arg ~arg-val specs#))))))

(arg-type no-arg :none)
(arg-type with-arg :required)
(arg-type optional-arg :optional)

;; Really opt-list just takes the specs returned by a group of
;; (option) calls and lumps them together, but it's given its own API
;; to leave room for it to become more sophisticated in future without
;; disrupting clients
(def opt-list hash-set)

;; Map the LongOpt int-enum back to nice Clojure keywords
(def long-opt-argmode {:none LongOpt/NO_ARGUMENT
                       :required LongOpt/REQUIRED_ARGUMENT
                       :optional LongOpt/OPTIONAL_ARGUMENT})

(defn build-getopt-fragment
  "Turn a simple spec-map into a getopt string fragment, by gluing
  together all of the short option names, and sticking the appropriate
  number of colons after any options that take parameters."
  ([spec]
     (let [suffix (case (:arg spec)
                        :none ""
                        :required ":"
                        :optional "::")
           {names :short-names} spec]
       (apply str (map str names (repeat suffix))))))

(defn get-long-opts
  "Take a single spec-map, and return a seq of LongOpt objects
representing the long options it's willing to take"
  ([spec]
     (let [{:keys [short-names long-names arg]} spec
           short (first short-names)
           arg-arg (long-opt-argmode arg)
           ;; try to emulate a short option if possible, just in case
           [buf alias] (if short
                         [nil (int (first short))]
                         [(StringBuffer.) 0])]
       (map #(LongOpt. % arg-arg buf alias) long-names))))

(defn parse-cmdline-from-specs
  ([specs argv & [prog-name]]
     (getopt-map
      (getopt-seq
       (make-getopt prog-name
                    (apply str (mapcat build-getopt-fragment specs))
                    (mapcat get-long-opts specs)
                    argv)))))

(defn merge-opt-map [specs getopt-map]
  (merge (into {} (for [{:keys [id names parse] :as spec} specs]
                    (when-let [args (seq (filter (comp 
                                                  (set names)
                                                  key)
                                                 getopt-map))]
                      {id (reduce into (map (comp parse val) 
                                            args))})))
         (select-keys getopt-map [:clojopts/more])))

(comment Sample usage
         (clojopts "clojopts"
                  ["-v" "--with-name" "clojopts"]
                  (with-arg ["n" "with-name"] "The name to use")
                  (no-arg "v" "Verbose mode" :id :verbose)))

(defn clojopts*
  ([prog-name argv & specs]
     (merge-opt-map specs
                    (parse-cmdline-from-specs specs argv prog-name))))

(defn desugar-spec [spec]
  (let [[type & more] spec
        [names [doc & opts]] (split-with (! string?) more)]
    `(~type ~(vec (map str names)) ~doc ~@opts)))

(defmacro clojopts
  "The main entry point for clojopts. Requires your program's
name (for output in usage and version messages), a seq of command-line
options, and any number of option specifiers. Returns a map of any
options contained in the command line. Currently discards(!) all
non-option arguments, but a future (pre-1.0) version will add them
under the key :clojopts/more.

Options are specified in the following format:
<(arg-type name+ docstring & options)>

See the README for further details."
  ([prog-name argv & specs]
     `(clojopts* ~prog-name ~argv ~@(vec (map desugar-spec specs)))))
