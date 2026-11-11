(ns doteur.config
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)))

(defn- compile-pattern [pattern]
  (if (str/includes? pattern "/")
    ; TODO: Patterns starting with / should only match files
    ; under the specific directory this came from (and, at
    ; that exact relative route)
    pattern

    (->> pattern
         (str "(?i)")
         (re-pattern))))

(defn compile-patterns [patterns]
  (map compile-pattern patterns))

(defn- read-ignore-file [file]
  (->> file
       slurp
       str/split-lines
       (compile-patterns)))

(defn ignored-file-patterns [dir]
  (concat
   (let [dot-ignore-file (io/file dir ".dotignore")]
     ; If there's a .dotignore file in the environments dir, use that
     (when (.exists dot-ignore-file)
       (read-ignore-file dot-ignore-file)))

   ; Always augment with our defaults
   (when-let [uri (io/resource ".dotignore")]
     (read-ignore-file uri))))

(defn pattern-matches? [pattern ^File file file-name]
  (if (string? pattern)
    (str/includes? (str file) pattern)
    (re-matches pattern file-name)))

(defn build-is-ignored [patterns]
  (fn ignored? [^File file]
    (let [file-name (.getName file)]
      (some #(pattern-matches? % file file-name) patterns))))

(defn compose-ignores [a? b?]
  ; NOTE: This means we will check the base ignored patterns twice... probably fine?
  (if (and a? b?)
    (fn ignored? [^File file]
      (or (a? file)
          (b? file)))

    (or a? b? (constantly false))))
