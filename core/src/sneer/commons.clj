(ns sneer.commons)

(defn produce! [fn-if-absent map-atom key]
  (let [existing (get @map-atom key)]
    (if (some? existing)
      existing
      (get
        (swap! map-atom update-in [key]
               (fn [existing2] (if (some? existing2) existing2 (fn-if-absent key))))
        key))))

(defn now [] (sneer.commons.Clock/now))
