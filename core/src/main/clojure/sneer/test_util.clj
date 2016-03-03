(ns sneer.test-util
  (:require
    [sneer.commons :refer [nvl loop-trace]]
    [clojure.core.async :refer [alt!! timeout filter> >!! <!! close! chan]])
  (:import [java.io File]))

(defn ->clj-map [o]
  (when (some? o)
    (into {} (map (fn [field] [(keyword (.getName field)) (.get field o)])
                  (.getFields (class o))))))

(defn tmp-file []
  (doto
    (File/createTempFile "test-" ".tmp")
    (.delete)))

(defn tmp-folder []
  (doto (tmp-file) (.mkdir)))

(def ^:private default-timeout 400)

(defn >!!?
  ([ch v]
    (>!!? ch v default-timeout))
  ([ch v timeout-millis]
    (alt!!
      (timeout timeout-millis) (throw (RuntimeException. (str "TIMEOUT putting: " v)))
      [[ch v]] :>!!?-return)))

(defn <!!?
  ([ch]
    (<!!? ch default-timeout))
  ([ch timeout-millis]
    (alt!!
      (timeout timeout-millis) :timeout
      ch ([v] v))))

(defn ->predicate [expected]
  (if (fn? expected) expected #(= % expected)))

(defn try? [pred current]
  (try
    [(pred current) nil]
    (catch Exception e
      [false e])))

(defn not-ok [msg last-value last-exception]
  (do
    (println msg last-value)
    (when last-exception
      (.printStackTrace last-exception))
    false))

(defn <wait-trace! [ch expected]
  (let [expected (nvl expected :nil)
        pred (->predicate expected)]
    (loop-trace [last-value "<none>"
                 last-exception nil]
      (let [current (<!!? ch)]
        (cond
          (= current :timeout) (not-ok "TIMEOUT. Last value emitted:" last-value last-exception)
          (nil? current)       (not-ok "COMPLETED/CLOSED. Last value emitted:" last-value last-exception)
          :else (let [[success exception] (try? pred current)]
                  (if success
                    true
                    (recur current (or exception last-exception)))))))))

(defn compromised
  ([ch] (compromised ch 0.7))
  ([ch failure-rate]
    (filter> (fn [_] (> (rand) failure-rate)) ch)))

(defn compromised-if [unreliable ch]
  (if unreliable
    (compromised ch)
    ch))

(defn pst [fn]
  (try (fn)
       (catch Exception e (.printStackTrace e))))

(defn <emits [expected]
  (fn [ch]
    (<wait-trace! ch expected)))

(defn closes [ch]
  (loop-trace [last-value "<none>"]
              (if-some [current (<!!? ch)]
                (cond (= current :timeout) (do
                                             (println "TIMEOUT. Last value emitted:" last-value)
                                             false)
                      (::error current) (do
                                          (println "ERROR:" (::error current))
                                          false)
                      :else
                      (recur current))
                true)))

(def => "Anything, just to prevent warnings 'symbol => undefined' from the midje fact macro.")

; (do (require 'midje.repl) (midje.repl/autotest))
