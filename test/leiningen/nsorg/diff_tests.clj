(ns leiningen.nsorg.diff-tests
  (:require [leiningen.nsorg.diff :as diff]
            [midje.sweet :refer :all]))

(fact
  "Adds 3 lines of context around changed lines, and groups context lines and changed lines into chunks"
  (diff/diff-chunks (clojure.string/join \newline ["0" "1" "2" "3" "4" "5" "6" "7" "8" "9"])
                    (clojure.string/join \newline ["0" "1" "2" "3" "x" "y" "6" "7" "8" "9"]))
  => [[{:type :context :line 1 :content "1"}
       {:type :context :line 2 :content "2"}
       {:type :context :line 3 :content "3"}]
      [{:type :diff    :line 4 :from "4" :to "x"}
       {:type :diff    :line 5 :from "5" :to "y"}]
      [{:type :context :line 6 :content "6"}
       {:type :context :line 7 :content "7"}
       {:type :context :line 8 :content "8"}]])

(fact
  "Handles overlapping contexts correctly"
  (diff/diff-chunks (clojure.string/join \newline ["0" "1" "2" "3" "4" "5" "6"])
                    (clojure.string/join \newline ["0" "1" "x" "y" "4" "z" "6"]))
  => [[{:type :context :line 0 :content "0"}
       {:type :context :line 1 :content "1"}]
      [{:type :diff    :line 2 :from "2" :to "x"}
       {:type :diff    :line 3 :from "3" :to "y"}]
      [{:type :context :line 4 :content "4"}]
      [{:type :diff    :line 5 :from "5" :to "z"}]
      [{:type :context :line 6 :content "6"}]])

(fact
  "Formats chunks and volors removed lines red, added lines green"
  (diff/format-diff [[{:type :context :line 1 :content "1"}
                      {:type :context :line 2 :content "2"}
                      {:type :context :line 3 :content "3"}]
                     [{:type :diff    :line 4 :from "4" :to "x"}
                      {:type :diff    :line 5 :from "5" :to "y"}]
                     [{:type :context :line 6 :content "6"}
                      {:type :context :line 7 :content "7"}
                      {:type :context :line 8 :content "8"}]])
  => (clojure.string/join \newline [" 1"
                                    " 2"
                                    " 3"
                                    "\033[31m-4\033[0m"
                                    "\033[31m-5\033[0m"
                                    "\033[32m+x\033[0m"
                                    "\033[32m+y\033[0m"
                                    " 6"
                                    " 7"
                                    " 8"]))
