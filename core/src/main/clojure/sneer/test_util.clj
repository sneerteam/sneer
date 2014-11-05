(ns sneer.test-util
  (:require [clojure.core.async :refer [alt!! timeout]]))

(defn <!!? [ch]
  (alt!!
    (timeout 200) ::timeout
    ch ([v] v)))
         
(defn >!!? [ch v]
  (alt!!
    (timeout 200) false
    [[ch v]] true))
