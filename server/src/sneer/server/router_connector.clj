(ns sneer.server.router-connector
  (:require
    [midje.sweet :refer :all]
    [clojure.core.async :as async :refer [to-chan chan close! go <! >!]]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.server.router :refer :all]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn start-connector [queue-size packets-in packets-out]
  (go-trace
    (loop [router (create-router queue-size)
           previous-tuple nil]
      (when-some [packet (<! packets-in)]
        (match packet

          {:send tuple :from sender :to receiver}
          (if (-sender-queue-full? router sender receiver)
            (do
              (>! packets-out {:cts false :to sender})
              (recur router previous-tuple))
            (do
              (>! packets-out {:cts true  :to sender})
              (enqueue! router sender receiver tuple)
              (let [tuple (peek-tuple-for router receiver)]
	              (if (= tuple previous-tuple)
                  (recur router previous-tuple)
                  (do
                    (>! packets-out {:send tuple :to receiver})
                    (recur router tuple))))))
           
          {:pop receiver}
          (do
            (pop-tuple-for! router receiver)
            (if-some [tuple (peek-tuple-for router receiver)]
              (do
                (>! packets-out {:send tuple :to receiver})
                (recur router tuple))
              (recur router previous-tuple)))
           
          :else
          (do
            (println "Weird packet received: " packet)
            (recur router previous-tuple)))))

    (close! packets-out)))

(tabular "Router Connector"
  (fact ?fact
     (let [max-q-size 2
           packets-in (to-chan ?packets-in)
           packets-out (chan)
           subject (start-connector max-q-size packets-in packets-out)]
       (->> packets-out (async/into []) <!!?) => ?packets-out))
  
  ?fact ?packets-in ?packets-out
  
  "No packets"
  []
  []
  
  "A tuple is sent"
  [{:send "Hello" :from :A :to :B}]
  [{:cts true :to :A} {:send "Hello" :to :B}]

  "Queue is full"
  [{:send "Hello" :from :A :to :B}              {:send "Hello2" :from :A :to :B} {:send "Hello3" :from :A :to :B}]
  [{:cts true :to :A} {:send "Hello" :to :B}    {:cts true :to :A}               {:cts false :to :A}]
  
  "B receives a tuple"
  [{:send "Hello" :from :A :to :B}           {:pop :B}]
  [{:cts true :to :A} {:send "Hello" :to :B}]

  "B receives a tuple with another enqueued"
  [{:send "Hello" :from :A :to :B}           {:send "Hello2" :from :A :to :B} {:pop :B}]
  [{:cts true :to :A} {:send "Hello" :to :B} {:cts true :to :A}               {:send "Hello2" :to :B}])
