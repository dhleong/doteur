(ns doteur.cli.add
  (:require
   [babashka.process :refer [shell]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [doteur.cli.util :refer [print-formatted]]))

(defn- format-uri [uri]
  (if (str/includes? uri ":")
    ; Probably a full URI
    uri

    ; Github abbreviation
    (str "git@github.com:" uri ".git")))

(defn extract-repo-name [uri]
  (some->> (re-seq #"^.*\/([^\\]+?)\.git$" uri)
           (first)
           (second)))

(defn command [{{:keys [envs-dir uri dry-run]} :opts}]
  (let [full-uri (format-uri uri)
        repo-name (extract-repo-name full-uri)
        dest-path (.getAbsolutePath (io/file envs-dir repo-name))
        cmd ["git" "clone" full-uri dest-path]]
    (when dry-run
      (print-formatted [:yellow "*** THIS IS A DRY RUN ***"]))
    (apply println cmd)

    (when-not dry-run
      (System/exit
       (:exit (apply shell {:continue true} cmd))))))
