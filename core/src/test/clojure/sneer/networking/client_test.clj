(ns sneer.networking.client-test
  (:require
    [midje.sweet :refer :all]
    [clojure.core.async :as async :refer [thread to-chan chan close! >!!]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.networking.client-new :refer :all]))

;  (do (require 'midje.repl) (midje.repl/autotest))

(def t1 {:id 1 :author :A :payload "1"})
(def t2 {:id 2 :author :A :payload "2"})
(def t3 {:id 3 :author :A :payload "3"})

(def tC {:id 42 :author :C :payload "42"})

(let [packets-in (chan)
      packets-out (chan)
      tuples-received (chan)
      subject (start-client :A packets-in packets-out tuples-received)
      toB (chan)
      toC (chan)
      toD (chan)]
  (connect-to-follower subject :B toB)
  (connect-to-follower subject :C toC)
  (connect-to-follower subject :D toD)
  
  (fact "A tuple is sent"
    (>!!? toB t1) ; Non-blocking
    (<!!? packets-out) => {:from :A :send t1 :to :B})

  (fact "A tuple is received"
    (>!!? packets-in {:send tC})
    (<!!? tuples-received) => tC
    (<!!? packets-out) => {:from :A :ack (:author tC) :id (:id tC)}))
