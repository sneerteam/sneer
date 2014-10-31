(ns sneer.tuple.queue
  (:require [sneer.commons :refer [produce!]]
            [sneer.core :refer [query-tuples]]
            [sneer.rx :refer [observe-for-io]]
            [sneer.async :refer [go-trace]]
            [rx.lang.clojure.core :as rx]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async :refer [<! >! >!! <!! chan go-loop alt!]]))

; reference from server protocol
#_(defn status-to [to follower state]
   {:intent :status-of-queues
    :to to
    :follower follower
    :highest-sequence-delivered (:highest-sequence-delivered state)
    :highest-sequence-to-send (highest-sequence-to-send state)
    :full? false})

(def reliable-client-tables
  [
   {:table :follower
    :columns [:id :int :autoincrement
              :puk :blob :unique
              :next-sequence-to-send :int]}

   {:table :follower-queue
    :columns [[:sequence :int :autoincrement]
              [:follower :int]
              [:tuple :int]]}])

(defprotocol QueueStore
  (-empty? [store to])
  (-peek [store to])
  (-enqueue [store to tuple])
  (-pop [store to]))

(def IMMEDIATELY (doto (async/chan)
                   async/close!))

(def NEVER (async/chan))

(defn new-retry-period []
  (async/timeout 3000))

(defn start-queue-transmitter [from to store tuples-in packets-in packets-out]
  (letfn [(next-packet []
            (when-some [{:keys [sequence tuple]} (-peek store to)]
              {:intent :send :from from :to to :sequence sequence :payload tuple}))]
    (go-trace
     (loop [send-period IMMEDIATELY]
       (alt! :priority true
         
         send-period
         ([_]
           (if-some [packet (next-packet)]
             (do
               (>! packets-out packet)
               (recur (new-retry-period)))
             (recur NEVER)))
       
         tuples-in
         ([tuple]
           (when tuple
             (let [first? (-empty? store to)]
               (-enqueue store to tuple)
               (recur (if first? IMMEDIATELY send-period)))))
       
         packets-in
         ([packet]
            (match packet
                   {:intent :status-of-queues :highest-sequence-to-send hsts}
                   (if-some [{:keys [sequence]} (-peek store to)]
                     (if (= hsts sequence)
                       (do
                         (-pop store to)
                         (recur IMMEDIATELY))
                       (do
                         (>! packets-out (assoc (next-packet) :reset true))
                         (recur (new-retry-period))))
                     (recur NEVER))
                   :else (recur send-period)))))
   
     (async/close! packets-out))))
