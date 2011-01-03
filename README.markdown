# clojopts
## What is it?

[clojopts][] is a Clojure command-line parsing library. Its
underlying parser is [java-getopt][], which is a direct port of
[GNU getopt][], so users familiar with standard Unix tools
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
                (optional-arg file f name "The file to use")
                (with-arg lines n "How many lines to read"))
    => {:file #<File ~/src/clojure/awesome.clj>, :lines 10}

### Detailed usage options

The main entry point for clojopts is the `(clojopts [prog-name argv & specs])` 
macro. It requires your program's name (for
output in usage and version messages), a seq of command-line options,
and any number of option specifiers. Returns a map of any options
contained in the command line. Currently **discards** all non-option
arguments, but a future (pre-1.0) version will add them under the key
`:clojopts/more`.

Options are specified in the following format:
`(arg-type name+ docstring & options)`
Supported values for arg-type are: no-arg, with-arg, and optional-arg.

At least one name must be supplied, the first of which will be used as
the key for arguments having any of these names. Names need not be
quoted, and must not include leading dashes. Single-character names
are used as short options; longer names as long options. For example,
`(no-arg verbose v "Verbose output")` allows the user to specify either
--verbose or -v, and supplies you with a :verbose key in either case.

The docstring is *REQUIRED*, both to enforce good documentation and to
use as a separator between the list of names/aliases and any
additional options. The docstring will be displayed to the user upon
request with --help, or when the user supplies invalid parameters.

Permitted options include:

* :default - the value to use if an option was supplied without an
  argument. Defaults to nil.
* :id - the key to use in the map returned after parsing. Normally
  this is the keyword version of the first name you supply, but you
  can override this behavior, for example to provide the user a --name
  option that you wish to refer to as :file.
* :user-name - For options with arguments: what to call the argument
  in the --help string; defaults to "ARG". For example, --help might
  display --file=NAME
* :parse - a custom parsing function to run on the option's value
  before returning it. See the :group key for details on how the parse
  function is applied.
* :type - instructs clojopts to coerce the argument value to a
  particular type before handing it to your parsing function (or
  returning it). Supported types are:
  * :int
  * :str
  * :boolean - return false if the option is not present, or is "no",
  "false", or "0"; return true otherwise, including if the option is
  present with no argument
  * :file (a java.io.File)
  * :guess - this is the default option, and causes clojopts to try to
  guess the type of the argument from its value. Currently it tries in
  the following order, but this is subject to change: int, double,
  file, boolean, string. Most notably, file is only used if the file
  specified actually exists, because almost any string is a valid
  filename.
* :group - this influences how clojopts behaves when a single option
  is given multiple times. With any group setting except for :list,
  the parse and type functions are called on individual option values;
  in :list mode they are called on the option list as a whole. Legal
  values are:
  * :last/first - ignore all but the specified element
  * :list - process the list as a whole
  * :map - parse each element of the list separately, and return the
  result as a list
  * :maybe-map - like :map, but single-element lists will be returned
  to you as a single non-list item. This is the default setting
  because it is easiest to use and is generally right, but :map will
  be provide more regular results if you want to handle one or more
  arguments similarly. 

[clojopts]: https://github.com/amalloy/clojopts
[java-getopt]: http://www.urbanophile.com/~arenn/hacking/download.html#getopt
[GNU getopt]: http://www.gnu.org/s/libc/manual/html_node/Getopt.html#Getopt
