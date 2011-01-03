(ns clojopts.core
  (:use (clojopts getopt util ui))
  (:require [clojopts.parse :as parse]))

(defmacro arg-type
  "Template for building versions of option (see above) with different
argument-required-ness parameters."
  [fname arg-val]
  `(defn ~fname {:arglists '~'([name+ doc specs])}
     ([names# doc# & specs#]
        (option names# doc# (apply hash-map :arg ~arg-val specs#)))))

(arg-type no-arg :none)
(arg-type with-arg :required)
(arg-type optional-arg :optional)

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