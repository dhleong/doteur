#!/usr/bin/env bb
(ns doteur.main
  (:require
   [babashka.cli :as cli]
   [clojure.string :as str]
   [doteur.cli.update :as update]))

(defn print-help [_]
  (println (str/trim "
Usage: doteur <subcommand> <options>

Subcommands:
  update   Synchronize .dotfiles directories
")))

(def ^:private commands
  [{:cmds ["update"] :fn update/command :coerce {:dry-run :boolean}}
   {:cmds []
    :fn print-help}])

(defn -main [& args]
  (cli/dispatch commands args))
