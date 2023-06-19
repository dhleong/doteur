#!/usr/bin/env bb
(ns doteur.main
  (:require
   [babashka.cli :as cli]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [doteur.cli.update :as update]))

(def ^:private default-home (System/getenv "HOME"))

(def args-spec {:envs-dir {:desc "Where to add/search for environments"
                           :ref "<path>"
                           :default (.getAbsolutePath
                                      (io/file default-home ".dotfiles"))
                           :default-desc "$HOME/.dotfiles"}
                :home-dir {:desc "Where dotfiles get linked"
                           :ref "<path>"
                           :default default-home
                           :default-desc "$HOME"}
                :dry-run {:desc "Don't touch the filesystem"
                          :alias :dry
                          :coerce :boolean}
                :help {:desc "Print this help"
                       :alias :h
                       :coerce :boolean}})

(defn print-help [_]
  (println (str/trim "
Usage: doteur <subcommand> <options>

Subcommands:
  update   Synchronize .dotfiles directories

Options:
")
           (str "\n" (cli/format-opts {:spec args-spec}))))

(def ^:private commands
  [{:cmds ["update"] :fn update/command}
   {:cmds []
    :fn print-help}])

(defn- wrap-command [cmd-fn]
  (fn wrapped-command [{{:keys [help]} :opts :as invocation}]
    (if help
      (print-help invocation)
      (cmd-fn invocation))))

(defn -main [& args]
  (-> (map (fn [command]
             (-> command
                 (update :fn wrap-command)
                 (assoc :spec args-spec)))
           commands)
      (cli/dispatch args)))
