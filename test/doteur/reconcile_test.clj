(ns doteur.reconcile-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [doteur.reconcile :refer [reconcile]]))

(deftest reconcile-test
  (testing "Find common root directories and link where shared"
    (is (= {"characters" {"amity" [:link ["owl-house" "characters" "amity"]]
                          "catra" [:link ["spop" "characters" "catra"]]}
            "houses" [:link ["owl-house" "houses"]]
            "princesses" [:link ["spop" "princesses"]]}

           (reconcile
             [{:root ["owl-house"]
               :fs {"characters" {"amity" [:file]}
                    "houses" {"owl" [:file]}}}

              {:root ["spop"]
               :fs {"characters" {"catra" [:file]}
                    "princesses" {"she-ra" [:file]}}}]))))

  (testing "Barf on file/directory collision"
    (is (thrown-with-msg?
          Exception #"File/directory collision: owl-house/characters \(directory\), spop/characters \(file\)"
          (reconcile
            [{:root ["owl-house"]
              :fs {"characters" {"amity" [:file]}}}

             {:root ["spop"]
              :fs {"characters" [:file]}}]))))

  (testing "Keep destination files in intermediate directories"
    (is (= {"characters" {"amity" [:file]
                          "catra" [:link ["spop" "characters" "catra"]]}
            "houses" [:file]
            "princesses" [:link ["spop" "princesses"]]}

           (reconcile
             [{:root ["destination"]
               :destination? true
               :fs {"characters" {"amity" [:file]}
                    "houses" {"owl" [:file]}}}

              {:root ["spop"]
               :fs {"characters" {"catra" [:file]}
                    "princesses" {"she-ra" [:file]}}}])))))
