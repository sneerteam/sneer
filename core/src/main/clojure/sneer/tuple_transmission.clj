(ns sneer.tuple-transmission
  (:require [sneer.commons :refer [produce!]]
            [sneer.core :refer [query-tuples]]
            [sneer.rx :refer [observe-for-io]]
            [sneer.async :refer [go!]]
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

(defn new-retry-timeout []
  (async/timeout 1000))

(defn start-queue-transmitter [from to store tuples-in packets-in packets-out]
  (letfn [(next-packet []
            (when-some [{:keys [sequence tuple]} (-peek store to)]
              {:intent :send :from from :to to :sequence sequence :payload tuple}))]
    (go!
     (loop [retry-timeout IMMEDIATELY]
     
       (alt! :priority true
         
         retry-timeout
         ([_]
           (if-some [packet (next-packet)]
             (do
               (>! packets-out packet)
               (recur (new-retry-timeout)))
             (recur NEVER)))
       
         tuples-in
         ([tuple]
           (when tuple
             (let [first? (-empty? store to)]
               (-enqueue store to tuple)
               (recur (if first? IMMEDIATELY retry-timeout)))))
       
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
                         (recur (new-retry-timeout))))
                     (recur NEVER))))))
   
     (async/close! packets-out))))

(defn new-ping-timeout []
  (async/timeout 20000))

(defn store-tuple [tuple-base tuple]
  (throw (Exception. "not implemented")))

(defn criteria-for-sub [sub]
  (throw (Exception. "not implemented")))

(defn start [tuple-base database from-server to-server own-puk]

  (let [from-queues (chan)
        queues (atom {}) ; follower -> queue
        create-queue (fn [follower]
                       (let [tuples-in (chan 1)
                             packets-in (chan (async/sliding-buffer 1))
                             packets-out (chan (async/sliding-buffer 1))
                             queue-process (start-queue-transmitter database tuples-in packets-in packets-out)]
                         (async/pipe packets-out from-queues)
                         {:tuples tuples-in :packets packets-in}))
        queue-for (partial produce! queues create-queue)
        query-tuples (fn [criteria] (query-tuples tuple-base criteria true))]

                                        ; 1. network loop
    (go-loop [ping-timeout (new-ping-timeout)]

      (alt!

        ping-timeout
        ([_]
           (>! to-server {:intent :ping})
           (recur (new-ping-timeout)))

        from-server
        ([packet]
           (match packet
                  {:intent :receive :from followee :sequence sequence :payload tuple}
                  (do
                    ; TODO: verify tuple before storing
                    (store-tuple tuple-base tuple)
                    (>! to-server {:intent :ack :to followee :sequence sequence})
                    (recur (new-ping-timeout)))
                  {:intent :status-of-queues :follower follower}
                  (do
                    (>! (:packets (queue-for follower)) packet)
                    (recur ping-timeout))
                  {:intent :pong}
                  (do
                    (println "PONG")
                    (recur ping-timeout))
                  :else
                  (do
                    (println "unknown packet" packet)
                    (recur ping-timeout))))

        from-queues
        ([packet]
           (do
             (>! to-server packet)
             (recur (new-ping-timeout))))))

                                        ; 2. feeds follower queues
    (->
     (query-tuples {"type" "sub" "audience" own-puk})
     observe-for-io
                                        ; (rx/filter expired?)
     (rx/subscribe
      (fn [sub]
        (let [follower (sub "author")]
          (->
           (query-tuples (criteria-for-sub sub))
           (rx/subscribe
            (fn [tuple]
              (>!! (:tuples (queue-for follower)) tuple))))))))))
