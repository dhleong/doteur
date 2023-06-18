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

(defn- enter-file [structure file]
  (-> structure
      (update :root conj file)
      (update :fs get file)))

(defn- enter-file-n [file structures]
  (keep #(enter-file % file) structures))

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
             (let [owners (get directories->owners file)
                   owned-structures (->> structures
                                         (filter
                                           (fn [{:keys [root]}]
                                             (some (partial = root) owners)))
                                         ; If the destination is a link,
                                         ; we can ignore it
                                         (remove
                                           (fn [{:keys [fs destination?]}]
                                             (let [next-file (get fs file)]
                                               (and destination?
                                                    (vector? next-file)
                                                    (= :link (first next-file)))))))]
               (cond
                 ; Happy case:
                 (<= (count owners) 1)
                 (assoc unified file (if (= root destination-root)
                                       [:file] ; Leave the file
                                       [:link (conj root file)]))

                 ; Reconcile!
                 (every? #(map? (get-in % [:fs file])) owned-structures)
                 (assoc unified file (->> owned-structures
                                          (enter-file-n file)
                                          reconcile))

                 ; Otherwise, there's a collision that we can't handle
                 :else
                 (throw (ex-info (describe-collision
                                   (->> owned-structures
                                        (enter-file-n file)))
                                 {})))))
           {}))))
