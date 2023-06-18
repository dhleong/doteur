(ns doteur.structure
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)
   (java.nio.file Files)))

(def ^:private file-separator-regex (re-pattern File/separator))

(def ^:private directory-disallow-list #{".git"})

(defn- in-disallow-list? [file-path]
  (some directory-disallow-list file-path))

(defn- directory? [^File file]
  (.isDirectory file))

(defn- file->path [^File file]
  (-> file
      (.getAbsoluteFile)
      (.getCanonicalPath)
      (str/split file-separator-regex)))

(defn- type-of-file [^File f]
  (let [p (.toPath f)]
    (if (Files/isSymbolicLink p)
      [:link (->> (Files/readSymbolicLink p)
                  (.toFile)
                  (file->path))]

      [:file])))

(defn build-fs-at-file [^File root-file]
  (let [root (file->path root-file)
        root-len (count root)]
    (->> (file-seq root-file)
         (remove directory?)
         (reduce
           (fn [m file]
             (let [full-path (file->path file)]
               (if (in-disallow-list? full-path)
                 m
                 (let [relative-path (subvec full-path root-len)]
                   (assoc-in m relative-path (type-of-file file))))))
           {}))))

(defn collect-at-path [path]
  (->> (io/file path)
       (.listFiles)
       (filter directory?)
       (pmap
         (fn [root-file]
           {:root (file->path root-file)
            :fs (build-fs-at-file root-file)}))))
