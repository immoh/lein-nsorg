(ns leiningen.nsorg
  "Organize ns forms in source files."
  (:require [clojure.java.io :as io]
            [clojure.stacktrace]
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

(defn summarize [{:keys [replace interactive]} {:keys [files errors problems replaces]}]
  (clojure.string/join ", " (keep identity
                                  [(format "Checked %s files" files)
                                   (when (and (zero? errors) (zero? problems))
                                     "all good!")
                                   (when (pos? errors)
                                     (format "failed to check %s files" errors))
                                   (when (and (pos? problems)
                                              (or (not replace) interactive))
                                     (format "found problems in %s files" problems))
                                   (when (pos? replaces)
                                     (format "fixed %s files" replaces))])))

(defn organize-ns-form! [file replace? interactive?]
  (let [path (relativize-path (.getAbsolutePath file))]
    (try
      (let [original-source (slurp file)
            modified-source (nsorg/rewrite-ns-form original-source)
            diff-chunks (diff/diff-chunks original-source modified-source)
            problem? (seq diff-chunks)]
        (when problem?
          (lein/info (format "in %s:" path))
          (lein/info (diff/format-diff diff-chunks))
          (lein/info))
        (let [replaced? (when (and problem?
                                   replace?
                                   (or (not interactive?)
                                       (prompt! "Replace?")))
                          (spit file modified-source)
                          true)]
          {:files    1
           :problems (if problem? 1 0)
           :replaces (if replaced? 1 0)}))
      (catch Throwable t
        (lein/warn (format "Failed to check path %s:" path))
        (lein/warn (with-out-str (clojure.stacktrace/print-stack-trace t)))
        {:errors 1}))))

(defn organize-ns-forms! [paths options]
  (reduce
    (fn [result file]
      (merge-with + result (organize-ns-form! file (:replace options) (:interactive options))))
    {:errors 0 :files 0 :problems 0 :replaces 0}
    (find-clojure-files paths)))

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
      (if (or (pos? (:errors result))
              (and (pos? (:problems result))
                   (not (:replace options))))
        (lein/abort summary)
        (lein/info summary)))))
