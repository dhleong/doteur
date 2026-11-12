(ns doteur.structure
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [doteur.config :as config])
  (:import
   (java.io File)
   (java.nio.file Files)))

(def ^:private file-separator-regex (re-pattern File/separator))

(def ^:private directory-disallow-list #{".git" ".cache"})

(defn- in-disallow-list? [file-path]
  (some directory-disallow-list file-path))

(defn- directory? [^File file]
  (and (.isDirectory file)
       (not (Files/isSymbolicLink (.toPath file)))))

(defn- deep-merge [& values]
  (if (every? map? values)
    (apply merge-with deep-merge values)
    (last values)))

(defn- non-symlinked-file-seq [ignored? dir]
  ; The default file-seq follows symlinks; we want to leave
  ; symlinks in-place, as-is!
  ; Ignored directories are pruned entirely, so we don't waste
  ; time walking eg: node_modules
  (tree-seq
   directory?
   (fn [^File d] (remove ignored? (.listFiles d)))
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
  (let [p (.toPath f)]
    (if (Files/isSymbolicLink p)
      [:link (->> (Files/readSymbolicLink p)
                  (.toFile)
                  (file->path))]

      (when (.exists f)
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

(defn- assoc-file-in-fs [root-len m file]
  (let [full-path (file->path file)]
    (if (in-disallow-list? full-path)
      m
      (let [relative-path (subvec full-path root-len)]
        (try
          (assoc-in m relative-path (type-of-file file))
          (catch IllegalArgumentException e
            (debug-vector-overwrite m relative-path file)
            (throw e)))))))

(defn build-fs-at-file [config ^File root-file]
  (let [{:keys [root ignored?]
         :or {root (file->path root-file)
              ignored? (constantly false)}} config
        root-len (count root)]
    (if (directory? root-file)
      (->> (non-symlinked-file-seq ignored? root-file)
           (remove directory?)
           (reduce
            (partial assoc-file-in-fs root-len)
            {}))

      (type-of-file root-file))))

(defn collect-at-path [{:keys [ignored-file-patterns]} path]
  (let [ignored? (config/build-is-ignored ignored-file-patterns)
        base-config {:ignored? ignored?}]
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
            (let [root-path (file->path root-file)
                  config (update base-config :ignored? config/compose-ignores
                                 (config/build-is-ignored
                                  (config/ignored-file-patterns
                                   root-file)))]
              (when-not (in-disallow-list? root-path)
                {:root root-path
                 :fs (build-fs-at-file config root-file)}))))
         (keep identity))))

(defn- build-relevant-fs
  "Like build-fs-at-file, but only descends into directories that also exist in
  `combined-source-fs`, to avoid walking huge dirs that we wouldn't try to
  link anything into anyway (eg node_modules)."
  [combined-source-fs root ^File file]
  (if (and (map? combined-source-fs)
           (directory? file))
    (reduce-kv
     (fn [fs file-name child-source-fs]
       (let [child-fs (build-relevant-fs
                       child-source-fs
                       (conj root file-name)
                       (io/file file file-name))]
         (cond-> fs
           (some? child-fs) (assoc file-name child-fs))))

     {}
     combined-source-fs)

    ; Not a directory; reuse build-fs-at-file
    (build-fs-at-file {:root root} file)))

(defn build-relevant-at-file [{:keys [deep?]} source-structures target-root-file]
  (let [combined-source-fs (apply deep-merge (map :fs source-structures))
        root-path (file->path target-root-file)
        build-fs (if deep?
                   (fn build-fs-deep [root-dir-name _child-source-fs]
                     (build-fs-at-file
                      {:root (conj root-path root-dir-name)}
                      (io/file target-root-file root-dir-name)))

                   (fn build-fs-shallow [root-dir-name child-source-fs]
                     (build-relevant-fs
                      child-source-fs
                      (conj root-path root-dir-name)
                      (io/file target-root-file root-dir-name))))
        files (->> combined-source-fs
                   (pmap
                    (fn [[root-dir-name child-source-fs]]
                      [root-dir-name
                       (build-fs
                        root-dir-name
                        child-source-fs)]))
                   (filter (fn [[_root-dir-name fs]]
                             (some? fs))))]
    {:root (file->path target-root-file)
     :fs (into {} files)}))

(comment
  (def dotfiles (let [structures-dir (io/file (System/getenv "HOME")
                                              ".dotfiles")
                      ignored-patterns (config/ignored-file-patterns
                                        structures-dir)]
                  (collect-at-path
                   {:ignored-file-patterns ignored-patterns}
                   structures-dir)))

  (def destination (build-relevant-at-file
                    {}
                    dotfiles
                    (io/file (System/getenv "HOME"))))

  (dissoc (get-in destination [:fs ".config"]) "gcloud")
  (get-in destination [:fs ".zshenv.local"])

  (def reconciled-fs #_{:clj-kondo/ignore [:unresolved-namespace]}
    (doteur.reconcile/reconcile
     (cons (assoc destination :destination? true)
           dotfiles)))

  #_{:clj-kondo/ignore [:unresolved-namespace]}
  (doteur.actions/resolve-actions
   {:destination-fs (:fs destination)
    :reconciled-fs reconciled-fs
    :strutures dotfiles})

  (->> (keys (:fs destination))
       (map (fn [root-name]
              {:name root-name
               :value (let [v (get-in destination [:fs root-name])]
                        (if (map? v)
                          [:directory (keys v)]
                          v))}))
       #_(clojure.pprint/pprint)))
