# lein-nsorg [![Build Status](https://travis-ci.org/immoh/lein-nsorg.svg?branch=master)](https://travis-ci.org/immoh/lein-nsorg) [![Dependencies Status](https://jarkeeper.com/immoh/lein-nsorg/status.svg)](https://jarkeeper.com/immoh/lein-nsorg)

Leiningen plugin for organizing `ns` form in a way that whitespace and comments are preserved.

Rules to apply are fully customizable; the default implementation

* sorts `:require`, `:require-macros`, `:use` and `:use-macros` libspecs alphabetically
* sorts `:import` class names alphabetically
* sorts `:exclude`, `:only`, `:refer`, `:refer-macros` and `:rename` options alphabetically

Also available as Clojure library: [nsorg](https://github.com/immoh/nsorg/)


## Installation

Dependency information:

```clj
[lein-nsorg "0.1.2"]
```

## Usage

```
lein nsorg
```

Example output:
```
...
in ./src/perf/clojure/clojure/java/perf_jdbc.clj:
   These test compare the raw performance (against an in-memory H2 database)
   for hand-crafted Java JDBC calls and various `query` and `reducible-query`
   calls."
-  (:require [criterium.core :as cc]
-            [clojure.java.jdbc :as sql])
-  (:import (java.sql Connection PreparedStatement ResultSet Statement ResultSetMetaData)))
+  (:require [clojure.java.jdbc :as sql]
+            [criterium.core :as cc])
+  (:import (java.sql Connection PreparedStatement ResultSet ResultSetMetaData Statement)))

 (defn calibrate []
   ;; 840ms
...

Checked 5 files, found problems in 3 files
```

### Paths

Paths to check can be given as arguments to the plugin:

```
lein nsorg src dev/src
```

If no locations are given default location is:

* `:source-paths` and `:test-paths` if plugin is run inside Leiningen project
* current directory if plugin is run outside Leiningen project


### Apply changes automatically

By default lein-nsorg prints diffs for suggested changes. Changes can be applied automatically to source files with the
following option:

```
lein nsorg --replace
```

### Interactive mode

Instead of applying changes automatically interactive mode asks for each file if suggestions should be applied or not:

```
lein nsorg --replace --interactive
```

## License

Copyright © 2018 Immo Heikkinen

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
