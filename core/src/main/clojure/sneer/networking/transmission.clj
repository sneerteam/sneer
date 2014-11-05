(ns sneer.networking.transmission
  (:require [sneer.async :refer [go-while-let go-trace IMMEDIATELY]]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async :refer [<! >! >!! <!! chan go-loop alts!]]))

(defn new-retry-timeout [] (async/timeout 3000))

(defn start-transciever [tuples-to-send tuples-received packets-out packets-in lease]
  "'A transceiver is a device comprising both a transmitter and a receiver which are combined and share common circuitry'"
  (let [channels [packets-in lease]]
      
    (go-trace
      (loop [tuple nil
             time-to-send nil]
        (let [channels (conj channels (if tuple time-to-send tuples-to-send))
              [c v] (alts! channels)]
          (case c
          
            tuples-to-send
            (recur v IMMEDIATELY)
          
            time-to-send
            (do
              (>! packets-out {:intent :send :tuple tuple})
              (recur tuple (new-retry-timeout)))
          
            lease
            :closed
          
          
          
            )
          )
      
      
        )
      ))
  
  
  (go-while-let [tuple (<! tuples-to-send)]
    (let [id (:id tuple)]
      (loop [time-to-send IMMEDIATELY]
        (alt! :priority true

          packets-in
          ([packet] (when packet
                      (match packet
                         {:intent :ack :id id} :ok
                         :else (recur time-to-send))))

          time-to-send
          ([_] (do
                 (>! packets-out {:intent :send :tuple tuple})
                 (recur (new-retry-timeout)))))))))
