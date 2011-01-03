(ns clojopts.getopt
  (:use (clojopts util help))
  (:import (gnu.getopt Getopt LongOpt)))

(def ^{:doc "Bind to true in order to prevent clojopts from calling
System/exit when --help is supplied; this kills swank as well, which
is kinda a pain."
       :dynamic true}
  *testing* true)

;; Map the LongOpt int-enum into nice Clojure keywords
(def long-opt-argmode {:none LongOpt/NO_ARGUMENT
                       :required LongOpt/REQUIRED_ARGUMENT
                       :optional LongOpt/OPTIONAL_ARGUMENT})

(defn get-long-opts
  "Take a single spec-map, and return a seq of LongOpt objects
representing the long options it's willing to take."
  ([spec]
     (let [{:keys [short-names long-names arg]} spec
           short (first short-names)
           arg-arg (long-opt-argmode arg)
           ;; try to emulate a short option if possible, just in case
           [buf alias] (if short
                         [nil (int (first short))]
                         [(StringBuffer.) 0])]
       (map #(LongOpt. % arg-arg buf alias) long-names))))

(defn show-help [specs]
  (print (help-string specs)))

(defmulti process-option
  (fn [name & more]
    (keyword "clojopts.getopt" name)))

(defmethod process-option :default
  [& args])

(defmethod process-option ::help
  [_ specs]
  (show-help specs)
  (when-not *testing*
    (System/exit 0)))                   ; The user just wanted help,
                                        ; so don't wake up the parent
                                        ; app

(defmethod process-option ::?
  [_ specs]
  (show-help specs)
  (throw (IllegalArgumentException. "Unrecognized option")))

(defmethod process-option ::version
  [_ specs]
  #_TODO)

(defn- read-opt
  "Read a single key-value pair from a getopt handle. This function handles most
  of the interop required for dealing with GNU GetOpt, as well as hiding away
  the different ways long and short options are treated."
  [{:keys [gnu-obj longopt-array specs]}]
  (let [opt-int (.getopt gnu-obj)]
    (when-not (= -1 opt-int)
      (let [opt-name (if-not (zero? opt-int)
                       (str (char opt-int))
                       (.getName (nth (seq longopt-array)
                                      (.getLongind gnu-obj))))
            opt-val (.getOptarg gnu-obj)]
        (process-option opt-name specs)
        [opt-name opt-val]))))

(defn- get-remaining-args
  "Once option parsing is done, get-remaining-args will return a seq of all the
  strings GNU GetOpt says are not options. It is an error to call this function
  before all options have been read."
  ([{:keys [gnu-obj argv]}]
     {:pre [(= -1 (.getopt gnu-obj))]}
     (drop (.getOptind gnu-obj) argv)))

(defn make-getopt
  "Construct a handle to a getopt parser object. The return value will be a map
containing a GNU GetOpt object, as well as various bookkeeping data necessary
for working with it. Clients should make no assumptions about the structure of
this map."
  [prog-name opt-string long-opts argv specs]
  (let [argv-array (into-array String (seq argv))
        longopt-array (into-array LongOpt (conj (seq long-opts)
                                                (LongOpt. "help" LongOpt/NO_ARGUMENT (StringBuffer.) 0)))]
    {:argv argv-array
     :longopt-array longopt-array
     :prog-name prog-name
     :gnu-obj (Getopt. prog-name
                       argv-array
                       opt-string
                       longopt-array)
     :specs specs}))

(defn getopt-seq
  "Turn iterative calls to gnu getopt into a seq of return values.
Returns a vector [option-list all-non-options]."
  ([getopt]
     [(doall                        ; make sure getopt side effects happen first
       (take-while identity
                   (repeatedly #(read-opt getopt))))
      (get-remaining-args getopt)]))

(defn getopt-map
  "Turn an option seq into a map of keyword=>[values] pairs."
  [[opt-seq non-opts]]
  (let [res (reduce
             (fn [m [opt arg]]
               (update-in m [opt]
                          (fnil conj []) arg))
             {} opt-seq)
        more (seq non-opts)]
    (if more
      (assoc res :clojopts/more non-opts)
      res)))
