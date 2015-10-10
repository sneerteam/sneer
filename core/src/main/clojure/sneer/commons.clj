(ns sneer.commons
  (:import [java.lang AutoCloseable]
           (java.io Closeable)
           (java.util Arrays Map)
           (clojure.lang PersistentQueue)))

(defprotocol Disposable
  (dispose [resource]))

(extend-protocol Disposable
  Closeable
  (dispose [closeable]
    (.close closeable))
  AutoCloseable
  (dispose [closeable]
    (.close closeable)))

(defmacro loop-trace
  "Same as loop but prints unhandled exception stack trace"
  [binding & forms]
  `(try
    (loop ~binding
      ~@forms)
    (catch Throwable ~'e
      (println "LOOP ERROR" ~'e)
      #_(print-throwable ~'e)
      (.printStackTrace ~'e))))

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

(def empty-queue PersistentQueue/EMPTY)

(defn byte-array= [^bytes a1 ^bytes a2]
  (Arrays/equals a1 a2))

(defn produce! [fn-if-absent map-atom key]
  (if-some [existing (get @map-atom key)]
    existing
    (do
      (monitor-enter map-atom)
      (try
        ;; this supports functions with side-effects
        (let [state @map-atom]
          (if-some [existing (get state key)]
            existing
            (let [new-value (fn-if-absent key)]
              (reset! map-atom (assoc state key new-value))
              new-value)))
        (finally
          (monitor-exit map-atom))))))

(defn loop-state [fn initial]
  (let [next (try (fn initial) (catch Throwable t (.printStackTrace t)))]
    (when (not= next :break)
      (recur fn (merge initial next)))))

(defn flip [f]
  (fn [x y] (f y x)))

(defn update-java-map [^Map jmap key fn]
  (let [old-value (.get jmap key)]
    (.put jmap key (fn old-value))))

(def descending (flip compare))

(defn submap? [sub super]
  (reduce-kv
    (fn [_ k v]
      (if (= v (get super k))
        true
        (reduced false)))
    true
    sub))

(defn nvl [v default]
  (if (some? v) v default))

(defn niy []
  (throw (RuntimeException. "Not Implemented Yet")))

