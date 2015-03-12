(ns sneer.test-util
  (:require
   [clojure.core.async :refer [alt!! timeout filter> >!! close! chan]]
   [rx.lang.clojure.core :as rx]))

(defn <!!?
  ([ch]
    (<!!? ch 200))
  ([ch timeout-millis]
    (alt!!
      (timeout timeout-millis) :timeout
      ch ([v] v))))

(defn >!!?
  ([ch v]
    (>!!? ch v 200))
  ([ch v timeout-millis]
    (alt!!
      (timeout timeout-millis) false
      [[ch v]] true)))

(defn compromised
  ([ch] (compromised ch 0.7))
  ([ch failure-rate]
    (filter> (fn [_] (> (rand) failure-rate)) ch)))

(defn compromised-if [unreliable ch]
  (if unreliable
    (compromised ch)
    ch))


(defn subscribe-chan [ch observable]
  (rx/subscribe observable
                #(>!! ch %)
                #(do
                   (println "Rx Error:" %)
                   (close! ch))
                #(close! ch)))

(defn observable->chan [observable]
  (doto (chan)
    (subscribe-chan observable)))
