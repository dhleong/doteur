(ns doteur.cli.add
  (:require
   [babashka.process :refer [shell]]
   [doteur.cli.util :refer [print-formatted]]
   [clojure.string :as str]))

(defn- format-uri [uri]
  (if (str/includes? uri ":")
    ; Probably a full URI
    uri

    ; Github abbreviation
    (str "git@github.com:" uri ".git")))

(defn command [{{:keys [envs-dir uri dry-run]} :opts}]
  (let [cmd ["git" "clone" (format-uri uri) envs-dir]]
    (when dry-run
      (print-formatted [:yellow "*** THIS IS A DRY RUN ***"]))
    (apply println cmd)

    (when-not dry-run
      (System/exit
        (:exit (apply shell {:continue true} cmd))))))
