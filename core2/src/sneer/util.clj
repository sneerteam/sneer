(ns sneer.util)

(defmulti handle (fn [_state event]
                   (event :type)))

(defn prepend [vec v]
  (into [v] vec))
