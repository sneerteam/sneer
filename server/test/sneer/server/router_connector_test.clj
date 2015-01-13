(ns sneer.server.router-connector-test
  (:require
    [midje.sweet :refer :all]
    [clojure.core.async :as async :refer [thread to-chan chan close! >!!]]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.server.router-connector :refer :all]))

;  (do (require 'midje.repl) (midje.repl/autotest))

(def t1 {"id" 1 "author" :A :payload "1"})
(def t2 {"id" 2 "author" :A :payload "2"})
(def t3 {"id" 3 "author" :A :payload "3"})

(tabular "Router Connector"
  (fact ?fact
     (let [max-q-size 2
           packets-in (chan)
           packets-out (chan)
           resend-timeout (chan)
           resend-timeout-fn (constantly resend-timeout)
           subject (start-connector max-q-size packets-in packets-out resend-timeout-fn)]
       (thread
         (doseq [event ?packets-in]
           (if (= event :resend)
             (>!! resend-timeout :stimulus)
             (>!! packets-in event)))
         (close! packets-in))
       (->> packets-out (async/into []) <!!?) => ?packets-out))
  
  ?fact ?packets-in ?packets-out
  
  "No packets"
  []
  []
  
  "A tuple is enqueued"
  [{:send t1 :from :A :to  :B}]
  [{:ack   1 :to   :A :for :B}]

  "A tuple is sent when client comes online (sends a ping)"
  [{:send t1 :from :A :to  :B} {:from :B} :resend]
  [{:ack   1 :to   :A :for :B} {:send t1 :to :B}]
  

  "Full queue emits NAK"
  [{:send t1 :from :A :to  :B} {:send t2 :from :A :to  :B} {:send t3 :from :A :to  :B}]
  [{:ack   1 :to   :A :for :B} {:ack   2 :to   :A :for :B} {:nak   3 :to   :A :for :B}]

  "B receives a tuple with another enqueued"
  [{:send t1 :from :A :to  :B} {:send t2 :from :A :to  :B} {:from :B} :resend {:ack :A :id 1 :from :B} :resend]
  [{:ack   1 :to   :A :for :B} {:ack   2 :to   :A :for :B} {:send t1 :to :B}  {:send t2      :to   :B}]

  "A is notified when its send queue for B is empty."
  [
   ; packets from :A to :B
   {:send t1 :from :A :to :B} {:send t2 :from :A :to :B} {:send t3 :from :A :to :B}
   ; acks from :B to :A
   {:ack :A :id 1 :from :B} {:ack :A :id 2 :from :B}
   :resend
  ]
  (fn [packets] (= (last packets) {:cts :B :to :A}))

  ":ack for :cts."
  [
   ; packets from :A to :B
   {:send t1 :from :A :to :B} {:send t2 :from :A :to :B} {:send t3 :from :A :to :B}
   ; acks from :B to :A
   {:ack :A :id 1 :from :B} {:ack :A :id 2 :from :B}
   :resend
   {:ack :B :from :A}
   :resend ;; Should not cause :cts to be re-sent.
   :resend
  ]
  [{:ack 1, :for :B, :to :A} {:ack 2, :for :B, :to :A} {:for :B, :nak 3, :to :A} {:cts :B, :to :A}])