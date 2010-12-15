(ns cljopts.core
  (:use cljopts.getopt
        [cljopts.util :only [keywordize]]
        [clojure.contrib.seq :only [separate]])
  (:import (gnu.getopt Getopt LongOpt)))

(defn- option [names doc & specs]
  (let [[name names] ((apply juxt (if (vector? names)
                                    [first identity]
                                    [identity vector]))
                      names),
        {:keys [arg default parse user-name id]
         :or {arg :none, parse identity,
              user-name name, id (keyword name)}}
        specs, 
        [short-names long-names] (separate #(= (.length %) 1) names)]
    (keywordize [name short-names
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

(def cljopts hash-set)

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
    [(map #(LongOpt. % arg-arg buf alias) long-names) buf]))
