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

(defn loop-state [fn initial]
  (let [next (try (fn initial) (catch Throwable t (.printStackTrace t)))]
    (when (not= next :break)
      (recur fn (merge initial next)))))

(defmacro while-let
  "Makes it easy to continue processing an expression as long as it is true"
  [binding & forms]
  `(loop []
     (when-let ~binding
       ~@forms
       (recur))))
