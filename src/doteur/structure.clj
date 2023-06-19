(ns doteur.structure
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [doteur.config :as config])
  (:import
   (java.io File)
   (java.nio.file Files)))

(def ^:private file-separator-regex (re-pattern File/separator))

(def ^:private directory-disallow-list #{".git"})

(defn- in-disallow-list? [file-path]
  (some directory-disallow-list file-path))

(defn- directory? [^File file]
  (and (.isDirectory file)
       (not (Files/isSymbolicLink (.toPath file)))))

(defn- non-symlinked-file-seq [dir]
  ; The default file-seq follows symlinks; we want to leave
  ; symlinks in-place, as-is!
  (tree-seq
    directory?
    (fn [^File d] (seq (.listFiles d)))
    dir))

(defn- file->path [^File file]
  (-> file
      (.toPath)
      (.toAbsolutePath)
      (.normalize)
      (.toString)
      (str/split file-separator-regex)))

(defn path->file [path]
  (str/join File/separator path))

(defn- type-of-file [^File f]
  (when (.exists f)
    (let [p (.toPath f)]
      (if (Files/isSymbolicLink p)
        [:link (->> (Files/readSymbolicLink p)
                    (.toFile)
                    (file->path))]

        [:file]))))

(defn- debug-vector-overwrite [m relative-path file]
  (println "Overwriting file in " relative-path ";"
           "actual file=" (.getAbsolutePath file) "; "
           "want to set " (type-of-file file))
  (println (loop [m m
                  remaining-path relative-path
                  at-path []]
             (let [segment (first remaining-path)
                   v (get m segment)]
               (if (vector? v)
                 (println "Value at " (conj at-path segment)
                          "= " v)
                 (recur v
                        (next remaining-path)
                        (conj at-path segment)))))))

(defn build-fs-at-file [config ^File root-file]
  (let [{:keys [root ignored?]
         :or {root (file->path root-file)
              ignored? (constantly false)}} config
        root-len (count root)]
    (if (directory? root-file)
      (->> (non-symlinked-file-seq root-file)
           (remove directory?)
           (remove ignored?)

           (reduce
             (fn [m file]
               (let [full-path (file->path file)]
                 (if (in-disallow-list? full-path)
                   m
                   (let [relative-path (subvec full-path root-len)]
                     (try
                       (assoc-in m relative-path (type-of-file file))
                       (catch IllegalArgumentException e
                         (debug-vector-overwrite m relative-path file)
                         (throw e)))))))
             {}))

      (type-of-file root-file))))

(defn collect-at-path [{:keys [ignored-file-patterns]} path]
  (let [ignored? (config/build-is-ignored ignored-file-patterns)
        config {:ignored? ignored?}]
    ; TODO We should respect local .dotignore files
    (->> (io/file path)
         (.listFiles)
         (filter directory?)
         (remove ignored?)

         ; Only select subdirs that have been git-cloned in
         ; TODO This may not be necessary...
         (filter (fn [directory]
                   (.exists (io/file directory ".git"))))

         (pmap
           (fn [root-file]
             (let [root-path (file->path root-file)]
               (when-not (in-disallow-list? root-path)
                 {:root root-path
                  :fs (build-fs-at-file config root-file)}))))
         (keep identity))))

(defn build-relevant-at-file [source-structures target-root-file]
  (let [relevant-root-dirs (->> source-structures
                                (mapcat (comp keys :fs))
                                (into #{}))
        files (->> relevant-root-dirs
                   (pmap
                     (fn [root-dir-name]
                       [root-dir-name
                        (build-fs-at-file
                          {:root (conj (file->path target-root-file) root-dir-name)}
                          (io/file target-root-file root-dir-name))]))
                   (filter (fn [[_root-dir-name fs]]
                             (some? fs))))]
    {:root (file->path target-root-file)
     :fs (into {} files)}))


(comment
  (def dotfiles (collect-at-path
                  {}
                  (io/file (System/getenv "HOME")
                           ".dotfiles")))

  (def destination (build-relevant-at-file
                     dotfiles
                     (io/file (System/getenv "HOME"))))

  (dissoc (get-in destination [:fs ".config"]) "gcloud")

  (->> (keys (:fs destination))
       (map (fn [root-name]
              {:name root-name
               :value (let [v (get-in destination [:fs root-name])]
                        (if (map? v)
                          [:directory (keys v)]
                          v))}))
       #_(clojure.pprint/pprint)))
