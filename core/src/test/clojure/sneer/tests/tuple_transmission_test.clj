(ns sneer.tests.tuple-transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple-transmission :refer [start-queue-transmitter QueueStore]]
            [clojure.core.async :as async :refer [chan >!!]]))

(defn dropping-chan []
  (chan (async/dropping-buffer 1)))

(defn persistent-queue [& [elements]]
  (reduce conj clojure.lang.PersistentQueue/EMPTY elements))

(defn empty-store []
  (let [state (atom {:q (persistent-queue) :sequence 0})
        enqueue (fn [{:keys [q sequence]} tuple]
                  {:q (conj q {:sequence sequence :tuple tuple}) :sequence (inc sequence)})]
    (reify QueueStore
      (-peek [store to]
        (-> @state :q peek))
      (-enqueue [store to tuple]
        (swap! state enqueue tuple)))))

(defn <!!? [ch]
  (async/alt!!
    (async/timeout 200) ([_] :timeout)
    ch ([v] v)))

(facts
 "about tuple transmission"

 (fact
  "when first tuple is received it's sent to the server")
 
 (fact
  "when restarting over store with tuples it starts sending stored tuples"
  
  (let [t1 {}
        own-puk :me
        peer-puk :peer
        tuples-in (dropping-chan)
        packets-in (dropping-chan)
        packets-out (chan)
        store (empty-store)
        subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
    (>!! tuples-in t1)    
    
    (fact "packets-out must be closed after we close tuples-in"
          (async/close! tuples-in)
          (<!!? (async/filter< nil? packets-out)) => nil)
    
    (let [tuples-in (dropping-chan)
          packets-in (dropping-chan)
          packets-out (chan)
          subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
      (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t1 :sequence 0}
      (async/close! tuples-in)))) 

 
 (fact
  "when ack is received tuple is deleted from store "))
