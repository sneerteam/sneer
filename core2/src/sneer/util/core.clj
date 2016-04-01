(ns sneer.util.core)

(defmulti handle (fn [_state event]
                   (event :type)))

(defn prepend [vec v]
  (into [v] vec))

(defn conj-vec [?vec v]
  (vec (conj ?vec v)))