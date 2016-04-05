(ns sneer.util.core)

(defmulti handle (fn [_state event]
                   (event :type)))

(defn prepend [vec v]
  (into [v] vec))

(defn conj-vec [?vec v]
  (vec (conj ?vec v)))

(defn remove-vec [coll v]
  (vec (remove #(= % v) coll)))

(defn assoc-some
  "Same as assoc but only for non-nil keys"
  [map ?key val]
  (if (some? ?key)
    (assoc map ?key val)
    map))
