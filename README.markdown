# clojopts
## What is it?

[clojopts][this] is a Clojure command-line parsing library. Its
underlying parser is [java-getopt][jgetopt], which is a direct port of
[GNU getopt][getopt], so users familiar with standard Unix tools
should be very comfortable using it. However, clojopts comes with many
features not available in raw getopt, making it more suitable for
Clojure programs.

## Program features
* Its results are returned as a Clojure map, instead of by mutation of
  global flag variables.
* It provides options for handling multiple occurrences of the same
  parameter.
* It parses the options supplied by the user to native Clojure types -
  there are many built-in parsers, or you can supply your own.
* Numerous options for customizing parsing behavior, with sensible
  defaults so the simple case is easy.

## Sample usage
    (def *argv* ["--name=~/src/clojure/awesome.clj"
                   "-n" "10"])
    (clojopts "clojopts"
                *argv*
                (optional-arg file f name "The file to use" :type :file)
                (with-arg lines n "How many lines to read" :type :int))
    => {:file #<File ~/src/clojure/awesome.clj>, :lines 10}

[this]: https://github.com/amalloy/clojopts
[jgetopt]: http://www.urbanophile.com/~arenn/hacking/download.html#getopt
[getopt]: http://www.gnu.org/s/libc/manual/html_node/Getopt.html#Getopt
