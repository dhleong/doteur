(ns doteur.cli.util
  (:require
   [io.aviso.ansi :as ansi]))

(defn print-formatted [v]
  (println (ansi/compose v)))

