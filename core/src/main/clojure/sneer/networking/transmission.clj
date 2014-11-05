(ns sneer.networking.transmission
  (:require [sneer.async :refer [go-trace IMMEDIATELY]]
            [clojure.core.async :as async :refer [<! >! >!! <!! chan go-loop alt!]]))

(defn new-retry-timeout [] (async/timeout 3000))

(defn start-queue-transmitter [to tuples-in packets-in packets-out]
  (go-trace
    (loop []
      (when-let [tuple (<! tuples-in)]
        
        (loop [time-to-send IMMEDIATELY]
          (alt! :priority true

            time-to-send
            ([_] (do
                   (>! packets-out {:intent :send :tuple tuple :to to})
                   (recur (new-retry-timeout))))

            packets-in
            ([packet] (when packet
                        (when-not (= (:id packet) (:id tuple))
                           (recur time-to-send))))))
 
        (recur)))

    (async/close! packets-out)))