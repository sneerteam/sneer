(ns sneer.server.router-connector-test
  (:require
    [midje.sweet :refer :all]
    [clojure.core.async :as async :refer [to-chan chan close! go <! >!]]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.server.router-connector :refer :all]))


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
  [{:send "Hello" :from :A :to :B}]
  [{:cts true :to :A :for :B}]

  "A tuple is sent"
  [{:send "Hello" :from :A :to :B} {:peek :B}]
  [{:cts true :to :A :for :B}      {:send "Hello" :to :B}]
  
  "Queue is full"
  [{:send "Hello" :from :A :to :B} {:send "Hello2" :from :A :to :B} {:send "Hello3" :from :A :to :B}]
  [{:cts true :to :A :for :B}      {:cts true :to :A :for :B}       {:cts false :to :A :for :B}]
  
  "B comes online and receives a tuple that was waiting"
  [{:send "Hello" :from :A :to :B} {:peek :B}]
  [{:cts true :to :A :for :B}      {:send "Hello" :to :B}]

  "B receives a tuple with another enqueued"
  [{:send "Hello" :from :A :to :B} {:send "Hello2" :from :A :to :B} {:peek :B}             {:pop :B}]
  [{:cts true :to :A :for :B}      {:cts true :to :A :for :B}       {:send "Hello" :to :B} {:send "Hello2" :to :B}]

  "A is notified when its send queue for B is empty."
  [{:send "Hello" :from :A :to :B} {:send "Hello2" :from :A :to :B} {:send "Hello3" :from :A :to :B} {:pop :B} {:pop :B}]
  (fn [packets] (= (last packets) {:cts true :to :A :for :B}))
)
