(ns doteur.config
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)))

(defn ignored-file-patterns [dir]
  (let [dot-ignore-file (io/file dir ".dotignore")]
    (when (.exists dot-ignore-file)
      (->> dot-ignore-file
           slurp
           str/split-lines
           (map (comp re-pattern (partial str "(?i)")))))))

(defn build-is-ignored [patterns]
  (fn ignored? [^File file]
    (let [file-name (.getName file)]
      (some #(re-matches % file-name) patterns))))
