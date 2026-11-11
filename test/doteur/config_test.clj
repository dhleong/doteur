(ns doteur.config-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [doteur.config :as config]))

(deftest ignored?-test
  (testing "Ignore by name"
    (let [ignored? (config/build-is-ignored
                    (config/compile-patterns
                     ["changelog.*"]))]
      (is (ignored? (io/file "changelog.txt")))
      (is (not
           (ignored? (io/file "changelor"))))))

  (testing "Ignore by path"
    (let [ignored? (config/build-is-ignored
                    (config/compile-patterns
                     ["/.local/share"]))]
      (is (ignored? (io/file "/Users/catra/.dotfiles/dots/.local/share/test"))))))

