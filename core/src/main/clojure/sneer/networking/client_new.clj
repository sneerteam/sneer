(ns sneer.networking.client-new
  (:require [clojure.core.async :refer [pipe map< map> mult tap filter<]])
  (:import [sneer.commons SystemReport]))

(defn- ->ack [tuple]
  {:ack (:author tuple) :id (:id tuple)})

(defn connect-to-follower [client follower-puk tuples-out]
  (pipe
    tuples-out
    (map> #(do {:send % :to follower-puk}) (:packets-out client))))

(defn start-client [own-puk packets-in packets-out tuples-received]
  (let [packets-out (map> #(assoc % :from own-puk) packets-out)
        tuples-in (->> packets-in (map< :send) (filter< some?) mult)]
    (tap tuples-in tuples-received)
    (tap tuples-in (map> ->ack packets-out))
    {:packets-out packets-out}))
