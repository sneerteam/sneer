(ns sneer.commons)

(defn produce [map-atom key fn-if-absent]
  (if-some [existing (get @map-atom key)]
    existing
    (let [new-value (fn-if-absent key)]
      (swap! map-atom assoc key new-value)
      new-value)))

(defn now []
  (System/currentTimeMillis))
