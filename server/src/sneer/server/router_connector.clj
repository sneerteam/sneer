(ns sneer.server.router-connector
  (:require
    [clojure.core.async :as async :refer [to-chan chan close! go <! >!]]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.server.router :refer :all]))


(defprotocol RouterConnector
  (connect [receiver connection])
  )


; (do (require 'midje.repl) (midje.repl/autotest))

(defn start-connector [queue-size packets-in packets-out]
  (go-trace
    (loop [router (create-router queue-size)]
      (when-some [packet (<! packets-in)]
        (match packet

          {:send tuple :from sender :to receiver}
          (if (queue-full? router sender receiver)
            (do
              (>! packets-out {:cts false :to sender :for receiver})
              (recur router))
            (do
              (>! packets-out {:cts true  :to sender :for receiver})
              (recur (enqueue! router sender receiver tuple))))
           
          {:pop receiver}
          (let [original router
                router (pop-packet-for router receiver)]
            (when-some [sender-to-notify (sender-to-notify original router receiver)]
              (>! packets-out {:cts true :to sender-to-notify :for receiver}))
            (when-some [tuple (peek-packet-for router receiver)]
              (>! packets-out (assoc tuple :to receiver))
              (recur router)))

          {:peek receiver}
          (do
            (when-some [tuple (peek-packet-for router receiver)]
              (>! packets-out (assoc tuple :to receiver)))
            (recur router))
          
          :else
          (do
            (println "Weird packet received: " packet)
            (recur router)))))

    (close! packets-out)))
