(ns clojopts.ui
  (:use (clojopts parse util getopt)))

(defn name-for-type [t]
  (get {:guess "arg"
        :int "int"
        :boolean "bool"
        :str "str"
        :file "file"}
       t "arg"))

(defn option
  "Takes a name (or vector of names), a docstring, and an optional set
of :option, value pairs, and returns an attribute map representing all
that information in a single (internal to clojopts) object."
  ([names doc specs]
     (let [[name names] ((apply juxt (if (vector? names)
                                       [first identity]
                                       [identity vector]))
                         names),
           {:keys [arg default id user-name]
            :or {arg :none, id (keyword name), user-name (name-for-type (:type specs))}}
           specs,
           parse (parse-fn specs)
           [short-names long-names] ((juxt filter remove) #(= (.length %) 1) names)]
       (keywordize [name names short-names
                    long-names arg user-name
                    parse default id doc]))))

;; Really opt-list just takes the specs returned by a group of
;; (option) calls and lumps them together, but it's given its own API
;; to leave room for it to become more sophisticated in future without
;; disrupting clients
(def opt-list vector)

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

(defn merge-opt-map [specs getopt-map]
  (merge (into {} (for [{:keys [id names parse] :as spec} specs]
                    (when-let [args (seq (filter (comp 
                                                  (set names)
                                                  key)
                                                 getopt-map))]
                      {id (reduce into (map (comp parse val) 
                                            args))})))
         (select-keys getopt-map [:clojopts/more])))

(defn parse-cmdline-from-specs
  ([specs argv & [prog-name]]
     (getopt-map
      (getopt-seq
       (make-getopt prog-name
                    (apply str (mapcat build-getopt-fragment specs))
                    (mapcat get-long-opts specs)
                    argv
                    specs)))))
