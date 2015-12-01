(ns sneer.networking.client-test
  (:require
   [midje.sweet :refer [fact]]
   [clojure.core.async :refer [thread to-chan chan close! >!! map>]]
   [sneer.test-util :refer :all]
   [sneer.async :refer :all]
   [sneer.networking.client :refer :all]))

;  (do (require 'midje.repl) (midje.repl/autotest))

(def t1 {"id" 1 "author" :A :payload "1"})
(def t2 {"id" 2 "author" :A :payload "2"})

(def tC {"id" 42 "author" :C :payload "42"})

(let [packets-in (chan)
      packets-out (chan)
      tuples-received (chan)
      subject (start-client :A packets-in packets-out tuples-received)
      resend-timeout (chan)
      resend-timeout-fn (constantly resend-timeout)
      ack-ch (sliding-chan)
      follower-chan #(->> (chan 2) (map> (fn [tuple] [tuple ack-ch])))
      toB (follower-chan)
      toC (follower-chan)
      toD (follower-chan)]

  (connect-to-follower subject :B toB resend-timeout-fn)
  (connect-to-follower subject :C toC resend-timeout-fn)
  (connect-to-follower subject :D toD resend-timeout-fn)
  
  (fact "A tuple is sent"
    (>!!? toB t1)
    (<!!? packets-out) => {:from :A :send t1 :to :B})

  (fact "A tuple is received"
    (>!!? packets-in {:send tC})
    (<!!? tuples-received) => tC
    (<!!? packets-out) => {:from :A :ack (get tC "author") :id (get tC "id")})

  (fact "Pending tuple is sent when :cts"
    (>!!? packets-in {:nak (get t1 "id") :for :B})
    (>!!? packets-in {:cts :B})
    (<!!? packets-out) => {:from :A :ack :B}
    (<!!? packets-out) => {:from :A :send t1 :to :B})

  (fact "Pending tuple is resent"
    (>!!? resend-timeout :stimulus)
    (<!!? packets-out) => {:from :A :send t1 :to :B})
  
  (fact "Next tuple is sent on :ack"
    (>!!? toB t2)
    (>!!? packets-in {:ack (get t1 "id") :for :B})
    (<!!? packets-out) => {:from :A :send t2 :to :B})

  (fact "Bogus packet doesn't interfere with resend"
    (>!!? packets-in {:bogus true :for :B})
    (>!!? resend-timeout :stimulus)
    (<!!? packets-out) => {:from :A :send t2 :to :B}))
