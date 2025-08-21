(ns doteur.cli.add-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [doteur.cli.add :refer [extract-repo-name]]))

(deftest extract-repo-name-test
  (testing "Get repo name from http URL"
    (is (= "dots" (extract-repo-name "https://github.com/amity/dots.git"))))

  (testing "Get repo name from ssh URI"
    (is (= "dots" (extract-repo-name "git@github.com/amity/dots.git")))))
