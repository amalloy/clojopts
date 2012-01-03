(defproject clojopts "0.3.1"
  :description "Command-line library for Clojure: a wrapper around GNU
  GetOpt, and some additional higher-level features."
  :url "https://github.com/amalloy/clojopts"
  :dependencies [[clojure "1.2.0"]
                 [useful "0.7.6-alpha1"]
                 [gnu.getopt/java-getopt "1.0.13"]]
  ; apache doesn't host getopt for some reason, so use sun's repo
  :repositories {"sun" "http://download.java.net/maven/2"})

