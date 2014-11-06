(ns sneer.networking.transmission
  (:require [sneer.async :refer [go-while-let go-trace IMMEDIATELY]]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async :refer [<! >! >!! <!! chan go-loop alts!]]))

(defn- handle-packet []
  )

(defn new-retry-timeout [] (async/timeout 3000))

(defn start-transciever [to-send received packets-out packets-in hash-fn lease]
  "'A transceiver is a device comprising both a transmitter and a receiver which are combined and share common circuitry'"
  (let [inputs [packets-in lease]]
    (go-trace
      (loop [packet-out nil
             time-to-send nil]
        (let [channels (conj inputs (if packet-out time-to-send to-send))
              [v c] (alts! channels)]
          (condp identical? c
          
            to-send
            (recur {:intent :send :data v} IMMEDIATELY)
          
            time-to-send
            (do
              (>! packets-out packet-out)
              (recur packet-out (new-retry-timeout)))
          
            lease
            :closed
          
            packets-in
            (do
              (match v
                {:intent :send :data data}
                (do
                  (>! received data)
                  (>! packets-out {:intent :ack :hash (hash-fn data)})
                  (recur packet-out time-to-send))
              
                {:intent :ack :hash hash}
                (do
                  (if (and packet-out (= hash (-> packet-out :data hash-fn)))
                    (recur nil nil)
                    (recur packet-out time-to-send)))

                :else
                (recur packet-out time-to-send)))))))))
