(ns doteur.actions
  (:require
   [clojure.data :refer [diff]]))

(defn- map-tree-seq [m]
  (->> m
       (tree-seq
         (fn [node]
           (not (and (vector? node)
                     (keyword? (first node)))))
         (fn [node]
           (cond
             (map? node)
             (map (fn [[k v]] (list [k] v))
                  node)

             (and (list? node)
                  (map? (second node)))
             (map (fn [[k v]]
                    (list (conj (first node) k)
                          v))
                  (second node)))))

       (filter #(and (list? %)
                     (vector? (second %))))))

(defn- vector-starts-with? [v subv]
  (let [subv-len (count subv)]
    (and (>= (count v) subv-len)
         (= subv (subvec v 0 subv-len)))))

(defn- managed-link? [structures file-type]
  (and (vector? file-type)
       (= :link (first file-type))
       (let [link (second file-type)]
         (some #(vector-starts-with? link (:root %)) structures))))

(defn- select-delete-actions [structures only-in-destination]
  (->> only-in-destination
       map-tree-seq

       (keep (fn [[path file-type]]
               ; We should only delete managed links
               ; If it's a [:file] then it's unmanaged
               (when (managed-link? structures file-type)
                 [:delete path])))))

(defn- select-link-actions [only-in-reconciled]
  (->> only-in-reconciled
       map-tree-seq

       (keep (fn [[path file-type]]
               ; NOTE: Any [:file] types in the reconciled fs can
               ; just stay there!
               (when (= :link (first file-type))
                 [:link path (second file-type)])))))

(defn resolve-actions [{:keys [destination-fs reconciled-fs structures]}]
  (let [[only-in-destination only-in-reconciled] (diff destination-fs reconciled-fs)]
    (concat
      (select-delete-actions structures only-in-destination)
      (select-link-actions only-in-reconciled))))
