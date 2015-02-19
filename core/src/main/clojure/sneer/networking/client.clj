(ns sneer.networking.client
  (:require [clojure.core.async :refer [>! <! map> timeout alt! chan]]
            [sneer.async :refer [go-while-let dropping-chan IMMEDIATELY]]
            [clojure.core.match :refer [match]])
  (:import [sneer.commons SystemReport]))

(def ^:private NEVER (chan))

(defn- author-of [tuple]
  (get tuple "author"))

(defn- id-of [tuple]
  (get tuple "id"))

(defn- ->ack [tuple]
  {:ack (author-of tuple) :id (id-of tuple)})

(defn- assoc-new [map k v]
  (assert (not (contains? map k)))
  (assoc map k v))

(defn- accept-packets-from! [client follower-puk packets-in]
  (swap! (:packets-in-by-follower client) assoc-new follower-puk packets-in)
  packets-in)

(defn connect-to-follower [client follower-puk tuples-out & [resend-timeout-fn]]
  (println "connect-to-follower" (:own-puk client) follower-puk)
  (assert (not= follower-puk (:own-puk client)))
  (let [follower-packets-in (accept-packets-from! client follower-puk (dropping-chan))
        packets-out (:packets-out client)
        resend-timeout-fn (or resend-timeout-fn #(timeout 5000))]
    (go-while-let [[tuple ack-ch] (<! tuples-out)]
      (SystemReport/updateReport
        (str "tuples/last-to-send/" follower-puk)
        tuple)

      (loop [resend-timeout IMMEDIATELY]
        (alt!
          follower-packets-in
          ([packet-in]
           (match packet-in
             {:ack id}
             (if (= id (id-of tuple))
               (do
                 (>! ack-ch tuple)
                 :break)
               (recur resend-timeout))

             {:nak id}
             (recur (if (= id (id-of tuple)) NEVER resend-timeout))

             {:cts _}
             (do
               (>! packets-out {:ack follower-puk})
               (recur IMMEDIATELY))

             :else
             (recur resend-timeout)))

          resend-timeout
          ([_]
            (>! packets-out {:send tuple :to follower-puk})
            (recur (resend-timeout-fn))))))))

(defn start-client [own-puk packets-in packets-out tuples-received]
  (let [packets-in-by-follower (atom {})
        packets-out (map> #(do
                             (SystemReport/updateReport "network/last-packet-out" %)
                             (assoc % :from own-puk))
                          packets-out)]
    
    (go-while-let [packet (<! packets-in)]
      (SystemReport/updateReport "network/last-packet-in" packet)
      (match packet
        {:send tuple}
        (do (>! tuples-received tuple)
            (>! packets-out (->ack tuple)))

        (:or {:for follower} {:cts follower})
        (if-some [follower-in (get @packets-in-by-follower follower)]
          (>! follower-in packet)
          (println "Dropping packet from disconnected follower:" packet))))

    {:own-puk own-puk
     :packets-out packets-out
     :packets-in-by-follower packets-in-by-follower}))
