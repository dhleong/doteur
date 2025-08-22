(ns doteur.templates
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)))

;; This is a map of "template definitions," keyed by filename.
;; :file - a function that returns expected full path to the template file
;; :contents - a predicate that returns true if the contents match the template
(def ^:private by-name
  {; The clojure cli seems to "helpfully" generate this file when you run it with an
   ; empty .clojure directory.
   "deps.edn" {:file #(io/file (System/getenv "HOME")
                               ".clojure"
                               "deps.edn")
               :contents (partial = "{
  :aliases {
    ;; Add cross-project aliases here
  }
}")}})

(defn is-template-file?
  "We describe a 'template file' as one that may exist without
   the User having put it there intentionally, and therefore
   may be ignored for the purposes of reconciliation."
  [{:keys [^File file contents]}]
  (when-some [template (get by-name (.getName file))]
    (and (= file ((:file template)))
         ((:contents template) (str/trim contents)))))
