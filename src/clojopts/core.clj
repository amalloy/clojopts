(ns clojopts.core
  (:use (clojopts getopt util ui)
        [useful.macro :only [macro-do]])
  (:require [clojopts.parse :as parse]))

(defn qualified-keyword [sym]
  (keyword "clojopts.core" (str sym)))

(defmulti read-spec
  "Clojopt specs are executable as functions, but during macro
processing it can be helpful to parse them without evaluating the
sexp. read-spec inspects the first argument of the sexp and calls the
appropriate spec parser with the remaining args. Example
use: (read-spec '(no-arg [\"name\"] \"the program name\"))"
  (fn [& [type]]
    (qualified-keyword type)))

(macro-do [fname arg-val]
  `(do
     (defn ~fname {:arglists '~'([name+ doc specs])}
       ([names# doc# & specs#]
          (option names# doc# (apply hash-map :arg ~arg-val specs#))))
     (defmethod read-spec ~(qualified-keyword fname)
       [~'& args#]
       (apply ~fname (rest args#))))
  no-arg :none, with-arg :required, optional-arg :optional)

(defn clojopts*
  ([prog-name argv & specs]
     (merge-opt-map specs
                    (parse-cmdline-from-specs specs argv prog-name))))

(defn desugar-spec [spec]
  (let [[type & more] spec
        [names [doc & opts]] (split-with (! string?) more)]
    `(~type ~(vec (map str names)) ~doc ~@opts)))

(defn desugar-specs* [specs]
  (vec (map desugar-spec specs)))

(defmacro desugar-specs [& specs]
  (desugar-specs* specs))

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
     `(clojopts* ~prog-name ~argv ~@(desugar-specs* specs))))

(defn with-options* [bindval specs & body]
  (let [ids (map (comp symbol :id) specs)]
    `(let [{:keys ~(vec ids)} ~bindval]
       ~@body)))

(defmacro with-options [prog-name argv specs & body]
  (with-options* ))