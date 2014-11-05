(ns sneer.networking.transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.networking.transmission :refer [start-queue-transmitter new-retry-timeout]]
            [sneer.async :refer :all]
            [sneer.test-util :refer :all]
            [clojure.core.async :refer [chan >!! <! go-loop close!]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(def peer-puk "Peer-Puk")

(def t0 {:id 0 "some-field" "First"})
(def t1 {:id 1 "some-field" "Next"})

(facts
  "About tuple transmission"

  (let [tuples-in (chan 2)
        packets-in (chan 1)
        packets-out (chan)
        retry-timeout (chan 1)]
    
    (with-redefs [new-retry-timeout (constantly retry-timeout)]
      (start-queue-transmitter peer-puk tuples-in packets-in packets-out)

      (>!! tuples-in t0)
      (>!! tuples-in t1)
    
      (fact
        "A tuple is sent to the server"
        (<!!? packets-out) => {:intent :send :to peer-puk :tuple t0})

      (fact
        "Sending is retried"
        (>!! retry-timeout ::stimulus)
        (<!!? packets-out) => {:intent :send :to peer-puk :tuple t0})
      
      (fact
        "When ack is received it starts sending next tuple"
        (>!! packets-in {:intent :ack
                         :follower peer-puk
                         :id 0})
        (<!!? packets-out) => {:intent :send :to peer-puk :tuple t1})  
    
    (fact
      "Channels out are closed when BOTH channels in are closed"
      (close! tuples-in)
      (close! packets-in)
      (<!!? packets-out) => nil))))