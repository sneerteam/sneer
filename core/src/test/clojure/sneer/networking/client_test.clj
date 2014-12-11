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
      resend-timeout (chan)
      resend-timeout-fn (constantly resend-timeout)
      toB (chan 2)
      toC (chan 2)
      toD (chan 2)]
  (connect-to-follower subject :B toB resend-timeout-fn)
  (connect-to-follower subject :C toC resend-timeout-fn)
  (connect-to-follower subject :D toD resend-timeout-fn)
  
  (fact "A tuple is sent"
    (>!!? toB t1) ; Non-blocking
    (<!!? packets-out) => {:from :A :send t1 :to :B})

  (fact "A tuple is received"
    (>!!? packets-in {:send tC})
    (<!!? tuples-received) => tC
    (<!!? packets-out) => {:from :A :ack (:author tC) :id (:id tC)})

  (fact "Pending tuple is sent when :cts"
    (>!!? packets-in {:nak (:id t1) :for :B})
    (>!!? packets-in {:cts :B})
    (<!!? packets-out) => {:from :A :send t1 :to :B})

  (fact "Pending tuple is resent"
    (>!!? resend-timeout :stimulus)
    (<!!? packets-out) => {:from :A :send t1 :to :B})
  
  (fact "Next tuple is sent on :ack"
    (>!!? toB t2)
    (>!!? packets-in {:ack (:id t1) :for :B})
    (<!!? packets-out) => {:from :A :send t2 :to :B}))
