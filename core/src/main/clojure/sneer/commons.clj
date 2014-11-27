(ns sneer.commons)

(def empty-queue clojure.lang.PersistentQueue/EMPTY)

(defn byte-array= [^bytes a1 ^bytes a2]
  (java.util.Arrays/equals a1 a2))

(defn produce! [fn-if-absent map-atom key]
  (let [existing (get @map-atom key)]
    (if (some? existing)
      existing
      (get
        (swap! map-atom update-in [key]
          (fn [existing2] (if (some? existing2) existing2 (fn-if-absent key))))
        key))))

(defn now [] (sneer.commons.Clock/now))
