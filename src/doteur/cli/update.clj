(ns doteur.cli.update
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [doteur.actions :as actions]
   [doteur.config :as config]
   [doteur.reconcile :as reconcile]
   [doteur.structure :as structure]))

(defn command [{{:keys [dry-run]} :opts}]
  (let [home-dir (System/getenv "HOME")
        structures-dir (io/file home-dir ".dotfiles")
        base-ignored-patterns (config/ignored-file-patterns
                                structures-dir)
        config {:ignored-file-patterns base-ignored-patterns}
        destination-dir (io/file home-dir)

        structures (structure/collect-at-path config structures-dir)
        destination (structure/build-relevant-at-file
                      structures
                      destination-dir)
        reconciled-fs (reconcile/reconcile
                           (cons (assoc destination :destination? true)
                                 structures))
        actions (actions/resolve-actions
                  {:destination-fs (:fs destination)
                   :reconciled-fs reconciled-fs
                   :structures structures})]

    (if dry-run
      (pprint/pprint actions)

      ; TODO
      (println actions))))
