(defproject cljopts "0.1.0-SNAPSHOT"
  :description "Command-line library for Clojure: a wrapper around GNU GetOpt, and some additional higher-level features."
  :dependencies [[clojure "1.2.0"]
                 [clojure-contrib "1.2.0"]
                 [gnu.getopt/java-getopt "1.0.13"]]
  ; apache doesn't host getopt for some reason, so use sun's repo
  :repositories {"sun" "http://download.java.net/maven/2"})

