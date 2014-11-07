(ns sneer.test-util
  (:require [clojure.core.async :refer [alt!! timeout filter>]]))

(defn <!!? [ch]
  (alt!!
    (timeout 200) :timeout
    ch ([v] v)))
         
(defn >!!? [ch v]
  (alt!!
    (timeout 200) false
    [[ch v]] true))

(defn compromised
  ([ch] (compromised ch 0.7))
  ([ch failure-rate]
    (filter> (fn [_] (> (rand) failure-rate)) ch)))

(defn compromised-if [unreliable ch]
  (if unreliable
    (compromised ch)
    ch))