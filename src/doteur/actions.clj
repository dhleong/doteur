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

(defn- is-link? [file-type]
  (and (vector? file-type)
       (= :link (first file-type))))

(defn- managed-link? [structures file-type]
  (and (is-link? file-type)
       (let [link (second file-type)]
         (some #(vector-starts-with? link (:root %)) structures))))

(defn- replaced-with-link? [path file-type reconciled-fs]
  (and (= [:file] file-type)
       (is-link? (get-in reconciled-fs path))))

(defn- relocated? [file-type]
  (nil? (first file-type)))

(defn- select-delete-actions [structures only-in-destination reconciled-fs]
  (->> only-in-destination
       map-tree-seq

       (keep (fn [[path file-type]]
               ; We should only delete managed links
               ; If it's a [:file] then it's unmanaged UNLESS the reconciled-fs turned it
               ; into a link (eg: the existing file was an exact match, or was only a "template")
               (when (or (managed-link? structures file-type)
                         (replaced-with-link? path file-type reconciled-fs)
                         (relocated? file-type))
                 [:delete path])))))

(defn- select-link-actions [only-in-reconciled]
  (->> only-in-reconciled
       map-tree-seq

       (keep (fn [[path file-type]]
               ; NOTE: Any [:file] types in the reconciled fs can
               ; just stay there!
               (when (= :link (first file-type))
                 [:link path (second file-type)])))))

(defn- update-link-actions [both reconciled-fs]
  (->> both
       map-tree-seq

       (keep (fn [[path file-type]]
               (when (and (= :link (first file-type))
                          ; nil in here means the root changed, eg:
                          ; [:link [nil "characters" "amity"]]
                          (some nil? (second file-type)))
                 (assert (= :link (first (get-in reconciled-fs path))))
                 ; Take the new path from the reconciled path
                 [:link path (second (get-in reconciled-fs path))])))))

(defn resolve-actions [{:keys [destination-fs reconciled-fs structures]}]
  (let [[only-in-destination only-in-reconciled both] (diff destination-fs
                                                            reconciled-fs)]
    (concat
     (select-delete-actions structures only-in-destination reconciled-fs)
     (select-link-actions only-in-reconciled)
     (update-link-actions both reconciled-fs))))
