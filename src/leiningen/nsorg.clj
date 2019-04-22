(ns leiningen.nsorg
  "Organize ns forms in source files."
  (:require [leiningen.core.main :as lein]
            [nsorg.cli :as cli]
            [nsorg.cli.terminal :as terminal]))

(defn ^:no-project-needed nsorg
  "Leiningen plugin for organizing ns forms in source files.

Usage: lein nsorg [OPTIONS] [PATHS]

Clojure files are searched recursively from given paths. If no paths are given
and Leiningen is run inside project, project source and test paths are used.
Otherwise current workign directory is used.

Options:
  -e, --replace       Apply organizing suggestions to source files.
  -i, --interactive   Ask before applying suggestion (requires --replace).
  -x, --exclude PATH  Path to exclude from analysis."
  [project & args]
  (binding [terminal/*info-fn* lein/info
            terminal/*error-fn* lein/warn]
    (let [{:keys [success? summary]} (cli/check args {:default-paths (mapcat #(get project %)
                                                                             [:source-paths :test-paths])})]
      (if success?
        (lein/info summary)
        (lein/abort summary)))))
