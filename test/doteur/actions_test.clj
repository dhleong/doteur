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

  (testing "Leave unmanaged files alone"
    (is (empty?
          (resolve-actions
            {:destination-fs {"characters" {"amity" [:file]
                                            "catra" [:link
                                                     ["spop"
                                                      "characters"
                                                      "catra"]]
                                            "kipo" [:link
                                                     [""
                                                      "kipo"]]}}
             :reconciled-fs {"characters" {"catra" [:link
                                                    ["spop"
                                                     "characters"
                                                     "catra"]]}}
             :structures [{:root ["spop"]}]}))))

  (testing "Expand link into directory"
    (is (= [[:delete ["characters"]]
            [:link ["characters" "catra"]
             ["spop" "characters" "catra"]]
            [:link ["characters" "amity"]
             ["owl-house" "characters" "amity"]]]

           (resolve-actions
             {:destination-fs {"characters" [:link ["spop" "characters"]]}
              :reconciled-fs {"characters" {"catra" [:link
                                                     ["spop"
                                                      "characters"
                                                      "catra"]]
                                            "amity" [:link
                                                     ["owl-house"
                                                      "characters"
                                                      "amity"]]}}
              :structures [{:root ["owl-house"]}
                           {:root ["spop"]}]})))))

