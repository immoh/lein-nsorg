(ns leiningen.nsorg.diff
  (:require [clojure.term.colors :as colors]))

(defn partition-chunks [lines]
  (->> lines
       (map-indexed vector)
       (partition-by (fn [[i {:keys [type line]}]] [type (- line i)]))
       (map (partial map second))))

(defn expand-context [lines distance]
  (set (for [line lines
             delta (range (- distance) (inc distance))]
         (+ line delta))))

(defn line-diffs [s1 s2]
  (let [ls1 (clojure.string/split-lines s1)
        ls2 (clojure.string/split-lines s2)
        diff-lines (set (filter identity (map (fn [i l1 l2] (when-not (= l1 l2) i)) (range) ls1 ls2)))
        context-lines (expand-context diff-lines 3)]
    (filter identity (map (fn [i l1 l2]
                            (cond
                              (diff-lines i) {:type :diff :line i :from l1 :to l2}
                              (context-lines i) {:type :context :line i :content l1}))
                          (range)
                          ls1
                          ls2))))

(defn context-line [s]
  (str " " s))

(defn removed-line [s]
  (colors/red (str "-" s)))

(defn added-line [s]
  (colors/green (str "+" s)))

(defn chunk->str [chunk]
  (clojure.string/join \newline (case (:type (first chunk))
                                  :context (map (comp context-line :content) chunk)
                                  :diff (concat (map (comp removed-line :from) chunk)
                                                (map (comp added-line :to) chunk)))))

(defn diff-chunks [s1 s2]
  (partition-chunks (line-diffs s1 s2)))

(defn format-diff [chunks]
  (clojure.string/join \newline (map chunk->str chunks)))
