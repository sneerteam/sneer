(ns sneer.commons)

(defprotocol Disposable
  (dispose [resource]))

(extend-protocol Disposable
  java.io.Closeable
  (dispose [closeable]
    (.close closeable)))

(defmacro while-let
  "Makes it easy to continue processing an expression as long as it is true"
  [binding & forms]
  `(loop []
     (when-let ~binding
       ~@forms
       (recur))))

(defmacro reify+
  "expands to reify form after macro expanding the body"
  [& body]
  `(reify ~@(map macroexpand body)))

(def empty-queue clojure.lang.PersistentQueue/EMPTY)

(defn byte-array= [^bytes a1 ^bytes a2]
  (java.util.Arrays/equals a1 a2))

(defn produce! [fn-if-absent map-atom key]
  (if-some [existing (get @map-atom key)]
    existing
    (get
     (swap! map-atom update-in [key]
            (fn [existing2] (if (some? existing2) existing2 (fn-if-absent key))))
     key)))

(defn now [] (sneer.commons.Clock/now))

(defn loop-state [fn initial]
  (let [next (try (fn initial) (catch Throwable t (.printStackTrace t)))]
    (when (not= next :break)
      (recur fn (merge initial next)))))
