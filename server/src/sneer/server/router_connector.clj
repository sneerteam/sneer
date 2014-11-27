(ns sneer.server.router-connector
  (:require
    [clojure.core.async :as async :refer [to-chan chan close! go <! >!]]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.commons :refer [empty-queue]]
    [sneer.server.router :refer :all]))


(defprotocol RouterConnector
  (connect [receiver connection])
  )

(def online-count (constantly 20))


; (do (require 'midje.repl) (midje.repl/autotest))

(defn- next-packet-to-send [state]
  (->>
    state
    :online-clients
    keys
    (map #(when-some [packet (peek-packet-for (:router state) %)]
           (assoc packet :to %)))
    (filter some?)
    first))

(defn start-connector [queue-size packets-in packets-out]
  (go-trace
    (loop [state {:router (create-router queue-size)
                  :online-clients {}
                  :send-round empty-queue}]
      (when-some [packet (next-packet-to-send state)]
        (>! packets-out packet))
      
      (when-some [packet (<! packets-in)]
        (if-some [from (:from packet)]
          (let [router (:router state)
                state (update-in state [:online-clients from :online-countdown] online-count)
                state (update-in state [:online-clients from :pending-to-send] #(if % % (peek-packet-for router from)))
                state (match packet
                        {:send tuple :from from :to to}
                        (do
                          (>! packets-out {:ack (:id tuple) :for to :to from})
                          (update-in state [:router] enqueue! from to tuple))
                         
                        :else
                        state)]
            (recur state))
          (recur state)))
      )

    (close! packets-out)))
