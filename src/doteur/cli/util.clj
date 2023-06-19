(ns doteur.cli.util
  (:require
   [io.aviso.ansi :as ansi]))

(defn print-formatted [v]
  #_{:clj-kondo/ignore [:unresolved-var]}
  (println (ansi/compose v)))

