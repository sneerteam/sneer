(ns sneer.server.router-connector-test
  (:require
    [midje.sweet :refer :all]
    [clojure.core.async :as async :refer [to-chan chan close! go <! >!]]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.server.router-connector :refer :all]))

; (do (require 'midje.repl) (midje.repl/autotest))

(def t1 {:id 1 :author :A :payload "1"})
(def t2 {:id 2 :author :A :payload "2"})
(def t3 {:id 3 :author :A :payload "3"})

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
  
  "A tuple is enqueued"
  [{:send t1 :from :A :to  :B}]
  [{:ack   1 :to   :A :for :B}]

  "A tuple is sent when client comes online (sends a ping)"
  [{:send t1 :from :A :to  :B} {:from :B}]
  [{:ack   1 :to   :A :for :B} {:send t1 :to :B}]

  "Full queue emits NAK"
  [{:send t1 :from :A :to  :B} {:send t2 :from :A :to  :B} {:send t3 :from :A :to  :B}]
  [{:ack   1 :to   :A :for :B} {:ack   2 :to   :A :for :B} {:nak   3 :to   :A :for :B}]

  "B receives a tuple with another enqueued"
  [{:send t1 :from :A :to  :B} {:send t2 :from :A :to  :B} {:from :B}        {:ack :A :id 1 :from :B}]
  [{:ack   1 :to   :A :for :B} {:ack   2 :to   :A :for :B} {:send t1 :to :B} {:send t2      :to   :B}]
  
  #_(
  "A is notified when its send queue for B is empty."
  [{:send "Hello" :from :A :to :B} {:send "Hello2" :from :A :to :B} {:send "Hello3" :from :A :to :B} {:pop :B} {:pop :B}]
  (fn [packets] (= (last packets) {:cts true :to :A :for :B}))
  )
)
