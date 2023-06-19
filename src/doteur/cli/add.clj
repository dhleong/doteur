(ns doteur.cli.add
  (:require
   [babashka.process :refer [shell]]
   [doteur.cli.util :refer [print-formatted]]))

(defn command [{{:keys [envs-dir uri dry-run]} :opts}]
  (let [cmd ["git" "clone" uri envs-dir]]
    (when dry-run
      (print-formatted [:yellow "*** THIS IS A DRY RUN ***"]))
    (apply println cmd)

    (when-not dry-run
      (System/exit
        (:exit (apply shell {:continue true} cmd))))))
