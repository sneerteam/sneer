(ns sneer.commons)

(defn produce! [fn-if-absent map-atom key]
  (get
    (swap! map-atom update-in [key]
           (fn [existing] (if (some? existing) existing (fn-if-absent key))))
    key))

(defn now [] (sneer.commons.Clock/now))
