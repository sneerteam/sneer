(ns sneer.test-util
  (:require
    [sneer.clojure.core :refer [nvl]]
    [clojure.core.async :refer [alt!! timeout filter> >!! close! chan]]
    [rx.lang.clojure.core :as rx]
    [sneer.rx :refer [observe-for-io]]))

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


(defn subscribe-chan [c observable]
  (rx/subscribe observable
                #(>!! c (nvl % :nil))  ; Channels cannot take nil
                #(do
                   (.printStackTrace %)
                   (close! c))
                #(close! c)))

(defn observable->chan [observable]
  (doto (chan)
    (subscribe-chan observable)))

(defn pst [fn]
  (try (fn)
       (catch Exception e (.printStackTrace e))))

(defn ->chan [^rx.Observable o]
  (->> o observe-for-io observable->chan))

; (do (require 'midje.repl) (midje.repl/autotest))