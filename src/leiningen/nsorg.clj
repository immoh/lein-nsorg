(ns leiningen.nsorg
  "Organize ns forms in source files."
  (:require [clojure.java.io :as io]
            [clojure.string]
            [clojure.tools.cli :as cli]
            [leiningen.core.main :as lein]
            [leiningen.nsorg.diff :as diff]
            [nsorg.core :as nsorg])
  (:import (java.io File)
           (java.nio.file Paths)))

(defn clojure-file? [^File file]
  (and (.isFile file)
       (re-matches #".+\.clj.?" (.getName file))))

(defn find-clojure-files [paths]
  (->> paths
       (map io/file)
       (mapcat file-seq)
       (filter clojure-file?)
       (sort-by (memfn getAbsolutePath))))

(defn prompt! [msg]
  (locking *out*
    (loop []
      (print (str msg " [y/N] "))
      (flush)
      (case (clojure.string/lower-case (read-line))
        ("y" "yes") true
        ("" "n" "no") false
        (recur)))))

(defn ->absolute-path [s]
  (.toAbsolutePath (Paths/get s (into-array String []))))

(defn relativize-path [path]
  (str (.relativize (->absolute-path "") (->absolute-path path))))

(defn summarize [{:keys [replace interactive]} {:keys [files problems replaces]}]
  (clojure.string/join ", " (keep identity
                                  [(format "Checked %s files" files)
                                   (cond
                                     (zero? problems) "all good!"
                                     (or (not replace) interactive) (format "found problems in %s files" problems))
                                   (when (pos? replaces)
                                     (format "fixed %s files" replaces))])))

(defn organize-ns-forms! [paths options]
  (loop [files (find-clojure-files paths)
         result {:files 0 :problems 0 :replaces 0}]
    (if-let [file (first files)]
      (let [path (relativize-path (.getAbsolutePath file))
            original-source (slurp file)
            modified-source (nsorg/rewrite-ns-form original-source)
            diff-chunks (diff/diff-chunks original-source modified-source)
            problem? (seq diff-chunks)]
        (when problem?
          (lein/info (format "in %s:" path))
          (lein/info (diff/format-diff diff-chunks))
          (lein/info))
        (let [replaced? (when (and problem?
                                   (:replace options)
                                   (or (not (:interactive options))
                                       (prompt! "Replace?")))
                          (spit file modified-source)
                          true)]
          (recur (rest files) (merge-with + result {:files    1
                                                    :problems (if problem? 1 0)
                                                    :replaces (if replaced? 1 0)}))))
      result)))

(defn get-paths [arguments project]
  (or (seq arguments)
      (seq (mapcat #(get project %) [:source-paths :test-paths]))
      ["./"]))

(defn ^:no-project-needed nsorg
  "Leiningen plugin for organizing ns forms in source files.

Usage: lein nsorg [OPTIONS] [PATHS]

Clojure files are searched recursively from given paths. If no paths are given
and Leiningen is run inside project, project source and test paths are used.
Otherwise current workign directory is used.

Options:
  -e, --replace      Apply organizing suggestions to source files.
  -i, --interactive  Ask before applying suggestion (requires --replace)."
  [project & args]
  (let [{:keys [options arguments]} (cli/parse-opts args [["-e" "--replace"] ["-i" "--interactive"]])
        paths (map relativize-path (get-paths arguments project))]
    (lein/info "Checking following paths:")
    (doseq [path (sort paths)]
      (lein/info path))
    (lein/info)
    (let [result (organize-ns-forms! paths options)
          summary (summarize options result)]
      (if (and (not (:replace options))
               (pos? (:problems result)))
        (lein/abort summary)
        (lein/info summary)))))
