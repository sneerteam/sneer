(ns sneer.tuple.queue-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple.queue :refer [start-queue-transmitter new-retry-timeout]]
            [sneer.async :refer :all]
            [clojure.core.async :as async :refer [chan >!! <! go-loop]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(def own-puk "Own-Puk")
(def peer-puk "Peer-Puk")

(def t0 {:id 0 "some-field" "First"})
(def t1 {:id 1 "some-field" "Next"})

(facts
  "About tuple transmission"

  (let [tuples-in (dropping-chan 2)
        packets-in (dropping-chan)
        packets-out (chan)
        retry-timeout (dropping-chan)]
    
    (with-redefs [new-retry-timeout (constantly retry-timeout)]
      (start-queue-transmitter own-puk peer-puk tuples-in packets-in packets-out)

      (>!! tuples-in t0)
      (>!! tuples-in t1)
    
      (fact
        "A tuple is sent to the server"
        (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :tuple t0})

      (fact
        "Sending is retried"
        (>!! retry-timeout ::stimulus)
        (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :tuple t0})
      
      (fact
        "When ack is received it starts sending next tuple"
        (>!! packets-in {:intent :ack
                         :to own-puk
                         :follower peer-puk
                         :id 0})
        (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :tuple t1})  
    
    (fact
      "Channels out are closed when channels in are closed"
      (async/close! tuples-in)
      (async/close! packets-in)
      (<!!? packets-out) => nil))))