(ns sneer.networking.client-new
  (:require [clojure.core.async :as async])
  (:import [sneer.commons SystemReport]))

(defn connect-to-follower 
  "client is what is returned by start-client, below.
  Throws IllegalState if there already is a connection to that follower."
  [client follower-puk tuples-out]

  (let [follower-packets-out (->> tuples-out (async/map< #(do {:send % :to follower-puk})))]
    (async/pipe follower-packets-out (:packets-out client))))

(defn start-client [own-puk packets-in packets-out tuples-received]
  {:packets-out (async/map> #(assoc % :from own-puk) packets-out)})
