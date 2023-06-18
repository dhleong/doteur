#!/usr/bin/env bb
(ns doteur.main
  (:require
   [doteur.reconcile :as reconcile]))

(defn -main [& args]
  (println "hi" (reconcile/reconcile args)))
