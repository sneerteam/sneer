(ns sneer.networking.client-new
  (:require [clojure.core.async :as async])
  (:import [sneer.commons SystemReport]))

(defn connect-to-follower 
  "client is what is returned by start-client, below.
  Throws IllegalState if there already is a connection to that follower."
  [client follower-puk tuples-out]
  (let [follower-packets-out (->> tuples-out (async/map< #(do {:send % :to follower-puk})))]
    (async/pipe follower-packets-out (:packets-out client))))

(defn ack-for-tuple [t]
  {:ack (:author t) :id (:id t)})

(defn start-client [own-puk packets-in packets-out tuples-received]
  (let [packets-out (async/map> #(assoc % :from own-puk) packets-out)
        tuples (->> packets-in (async/map< :send) (async/filter< some?))
        mult-tuples (async/mult tuples)]
    (async/tap mult-tuples tuples-received)
    (async/tap mult-tuples (async/map> ack-for-tuple packets-out))
    {:packets-out packets-out}))
