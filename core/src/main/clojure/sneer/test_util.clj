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

(defn compromised [ch]
  (filter> (fn [_] (> (rand) 0.7)) ch))

(defn compromised-if [unreliable ch]
  (if unreliable
    (compromised ch)
    ch))