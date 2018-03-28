(defproject lein-nsorg "0.2.0-SNAPSHOT"
  :description "Leiningen plugin for organizing ns form"
  :url "https://github.com/immoh/lein-nsorg"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clojure-term-colors "0.1.0"]
                 [nsorg "0.1.0"]
                 [org.clojure/tools.cli "0.3.5"]]
  :profiles {:dev {:dependencies [[midje "1.9.1"]]
                   :plugins [[lein-midje "3.2.1"]]}}
  :eval-in-leiningen true)
