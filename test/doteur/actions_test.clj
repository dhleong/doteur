(ns doteur.actions-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [doteur.actions :refer [resolve-actions]]))

(deftest resolve-actions-test
  (testing "Delete no-longer existing links"
    (is (= [[:delete ["characters" "angella"]]]

           (resolve-actions
             {:destination-fs {"characters" {"angella" [:link
                                                        ["spop"
                                                         "characters"
                                                         "angela"]]
                                             "catra" [:link
                                                      ["spop"
                                                       "characters"
                                                       "catra"]]}}
              :reconciled-fs {"characters" {"catra" [:link
                                                     ["spop"
                                                      "characters"
                                                      "catra"]]}}
              :structures [{:root ["spop"]}]}))))

  (testing "Leave unmanaged files alone"))

