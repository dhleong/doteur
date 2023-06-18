(ns doteur.reconcile
  (:require
   [clojure.string :as str]))

(defn- files-with-root [structures]
  (->> structures
       (mapcat (fn [{:keys [fs root]}]
                 (map vector
                      (keys fs)
                      (repeat root))))))

(defn- index-directories->owners [structures]
  (->> structures
       files-with-root

       (reduce
         (fn [m [directory root]]
           (update m directory (fnil conj #{}) root))
         {})))

(defn- describe-collision [structures]
  (str "File/directory collision: "
       (->> structures
            (map (fn [{:keys [fs root]}]
                   (str
                     (str/join "/" root)
                     (if (vector? fs)
                       (str " (" (name (first fs)) ")")
                       " (directory)"))))
            (str/join ", "))))

(defn reconcile
  "Given a collection of structures, 'unify' them with the minimal, shared
   set of folders."
  [structures]
  (let [directories->owners (index-directories->owners structures)
        destination-root (->> structures
                              (filter :destination?)
                              first
                              :root)]
    (->> structures
         files-with-root
         (reduce
           (fn [unified [file root]]
             (let [owners (get directories->owners file)]
               (cond
                 ; Happy case:
                 (<= (count owners) 1)
                 (assoc unified file (if (= root destination-root)
                                       [:file] ; Leave the file
                                       [:link (conj root file)]))

                 ; Reconcile!
                 :else
                 (let [to-reconcile
                       (->> structures
                            (filter (fn [{:keys [root]}]
                                      (some (partial = root) owners)))
                            (keep (fn [structure]
                                    (-> structure
                                        (update :root conj file)
                                        (update :fs get file)))))]
                   (when (some (comp vector? :fs) to-reconcile)
                     (throw (ex-info (describe-collision to-reconcile)
                                     {})))
                   (assoc unified file (reconcile to-reconcile))))))
           {}))))
