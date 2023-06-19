(ns doteur.cli.update
  (:require
   [clojure.java.io :as io]
   [doteur.actions :as actions]
   [doteur.cli.util :refer [print-formatted]]
   [doteur.config :as config]
   [doteur.reconcile :as reconcile]
   [doteur.structure :as structure :refer [path->file]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn describe-action [[action-type path alt-path]]
  (case action-type
    :delete [:plain
             [:red-bg "  RM  "]
             " "
             path]

    :link [:plain
           [:cyan-bg.black " LINK "]
           " "
           path [:cyan " -> "] alt-path]))

(defn- perform-action [[action-type path alt-path]]
  (case action-type
    :delete (io/delete-file path)
    :link (Files/createSymbolicLink
            (.toPath (io/file path))
            (.toPath (io/file alt-path))
            (make-array FileAttribute 0))))

(defn- inflate-dest-path [destination-dir path]
  (.getAbsolutePath
    (io/file
      destination-dir
      (path->file path))))

(defn- inflate-actions [destination-dir actions]
  (map (fn [[action-type path alt-path]]
         (let [path (inflate-dest-path destination-dir path)]
           (case action-type
             :delete [:delete path]
             :link [:link path (path->file alt-path)])))
       actions))

(defn command [{{:keys [home-dir envs-dir dry-run]} :opts}]
  (let [structures-dir (io/file envs-dir)
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
                   :structures structures})
        actions (inflate-actions destination-dir actions)]

    (when dry-run
      (print-formatted [:yellow "*** THIS IS A DRY RUN ***"]))

    (if (seq actions)
      (doseq [action actions]
        (print-formatted (describe-action action))

        (when-not dry-run
          (perform-action action)))

      (print-formatted
        [:green "🎉 Up to date! 🎉"]))

    (when dry-run
      (print-formatted [:yellow "*** THIS WAS A DRY RUN ***"]))))
