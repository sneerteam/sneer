(ns sneer.networking.client-new
  (:require [clojure.core.async :refer [>! <! map< map> mult tap filter< go-loop timeout alt!]]
            [sneer.async :refer [go-while-let dropping-chan]]
            [clojure.core.match :refer [match]])
  (:import [sneer.commons SystemReport]))

(defn- ->ack [tuple]
  {:ack (:author tuple) :id (:id tuple)})

(defn- connect! [client follower-puk]
  (let [connection (dropping-chan)]
    (swap! (:follower-channels client) assoc follower-puk connection)
    connection))

(defn connect-to-follower [client follower-puk tuples-out & [resend-timeout-fn]]
  (let [follower-packets (connect! client follower-puk)
        packets-out (:packets-out client)
        resend-timeout-fn (or resend-timeout-fn #(timeout 5000))]
    (go-while-let
     [tuple (<! tuples-out)]
     (loop []
       (>! packets-out {:send tuple :to follower-puk})
       (alt!
         follower-packets
         ([packet]
          (match packet
            {:ack id}
            (when-not (= id (:id tuple))
              (recur))

            {:nak id}
            ;;TODO: don't retry until :cts
            (recur)))

         (resend-timeout-fn)
         ([_]
          (recur)))))))

(defn start-client [own-puk packets-in packets-out tuples-received]
  (let [follower-channels (atom {})
        packets-out (map> #(assoc % :from own-puk) packets-out)]
    (go-while-let
     [packet (<! packets-in)]
     (match packet
       {:send tuple}
       (do (>! tuples-received tuple)
           (>! packets-out (->ack tuple)))

       {:for follower}
       (if-some [follower-channel (get @follower-channels follower)]
         (>! follower-channel packet)
         (println "Dropping packet for disconnected follower"))

       {:cts follower}
       ;;TODO: redirect to follower channel as above
       nil))
    {:packets-out packets-out
     :follower-channels follower-channels}))
