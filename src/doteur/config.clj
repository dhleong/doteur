(ns doteur.config
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)))

(defn- read-ignore-file [file]
  (->> file
       slurp
       str/split-lines
       (map (comp re-pattern (partial str "(?i)")))))

(defn ignored-file-patterns [dir]
  (or (let [dot-ignore-file (io/file dir ".dotignore")]
        ; If there's a .dotignore file in the environments dir, use that
        (when (.exists dot-ignore-file)
          (read-ignore-file dot-ignore-file)))

      ; If not, load some defaults
      (when-let [uri (io/resource ".dotignore")]
        (read-ignore-file uri))))

(defn build-is-ignored [patterns]
  (fn ignored? [^File file]
    (let [file-name (.getName file)]
      (some #(re-matches % file-name) patterns))))
