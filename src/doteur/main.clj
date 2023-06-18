#!/usr/bin/env bb
(ns doteur.main
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [doteur.config :as config]
   [doteur.reconcile :as reconcile]
   [doteur.structure :as structure]))

(defn -main [& args]
  (let [home-dir (System/getenv "HOME")
        structures-dir (if (seq args)
                         (first args)
                         (io/file home-dir ".dotfiles"))
        base-ignored-patterns (config/ignored-file-patterns
                                structures-dir)
        config {:ignored-file-patterns base-ignored-patterns}

        structures (structure/collect-at-path config structures-dir)
        destination (structure/build-relevant-at-file
                      structures
                      (io/file home-dir))
        reconciled-state (reconcile/reconcile
                           (cons (assoc destination :destination? true)
                                 structures))]
    (pprint/pprint reconciled-state)))
