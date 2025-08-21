(ns doteur.cli.add
  (:require
   [babashka.process :refer [sh shell]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [doteur.cli.util :refer [print-formatted]]))

(defn- resolve-github-username [uri]
  (let [username (str/trim (:out (sh ["gh" "api" "user" "--jq" ".login"])))]
    (if (seq username)
      username

      (do
        (print-formatted [:red "Unable to resolve username."])
        (print-formatted [:plain "Try using `" [:bold "doteur add username/" uri] "` instead"])
        (System/exit 1)))))

(defn- format-uri [uri]
  (cond
    ; Probably a full URI
    (str/includes? uri ":")
    uri

    ; Github abbreviation
    (str/includes? uri "/")
    (str "git@github.com:" uri ".git")

    ; Github repo name owned by the user
    :else
    (let [username (resolve-github-username uri)]
      (str "git@github.com:" username "/" uri ".git"))))

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
