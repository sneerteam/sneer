(ns sneer.tests.tuple-transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple-transmission :refer [start-queue-transmitter QueueStore new-retry-timeout]]
            [clojure.core.async :as async :refer [chan >!!]]))

(defn dropping-chan [& [n]]
  (chan (async/dropping-buffer (or n 1))))

(defn sliding-chan [& [n]]
  (chan (async/sliding-buffer (or n 1))))

(defn persistent-queue [& [elements]]
  (reduce conj clojure.lang.PersistentQueue/EMPTY elements))

(defn empty-store []
  (let [state (atom {:q (persistent-queue) :sequence 0})
        enqueue (fn [{:keys [q sequence]} tuple]
                  {:q (conj q {:sequence sequence :tuple tuple}) :sequence (inc sequence)})]
    (reify QueueStore
      (-empty? [store to]
        (-> @state :q empty?))
      (-peek [store to]
        (-> @state :q peek))      
      (-enqueue [store to tuple]
        (swap! state enqueue tuple))
      (-pop [store to]
        (swap! state update-in [:q] pop)))))

(defn <!!? [ch]
  (async/alt!!
    (async/timeout 200) ([_] :timeout)
    ch ([v] v)))

(def own-puk :me)

(def peer-puk :peer)

(def t0 {:tag :first})

(def t1 {:tag :next})

(facts
 "about tuple transmission" 
 
 (let [tuples-in (dropping-chan)
       packets-in (dropping-chan)
       packets-out (chan)
       store (empty-store)
       subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
    (>!! tuples-in t0)    
    
    (fact
      "when first tuple is received it's sent to the server"
      (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t0 :sequence 0})
    
    (fact
      "packets-out must be closed when tuples-in is closed"
      (async/close! tuples-in)
      (<!!? packets-out) => nil)
  
    (fact
      "when restarting over store with tuples it starts sending stored tuples"
      (let [tuples-in (dropping-chan)
            packets-in (dropping-chan)
            packets-out (chan)
            subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
        (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t0 :sequence 0}
        (async/close! tuples-in)))
    
    (fact
      "when ack is received it starts sending next tuple"
      (let [tuples-in (dropping-chan)
            packets-in (dropping-chan)
            packets-out (chan)
            subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
        (>!! tuples-in t1)        
        (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t0 :sequence 0}
        (>!! packets-in {:intent :status-of-queues
                         :to own-puk
                         :follower peer-puk
                         :highest-sequence-delivered -1
                         :highest-sequence-to-send 0
                         :full? false})
        (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t1 :sequence 1}
        (async/close! tuples-in))))
 
     (fact
      "when server is out of sync it sends a reset"
      (let [tuples-in (dropping-chan 2)
            packets-in (dropping-chan 2)
            packets-out (sliding-chan)
            store (empty-store)
            subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
        (>!! tuples-in t0)
        (>!! tuples-in t1)        
        (>!! packets-in {:intent :status-of-queues
                         :to own-puk
                         :follower peer-puk
                         :highest-sequence-delivered 0
                         :highest-sequence-to-send 0
                         :full? false})
        ;server restarted
        (>!! packets-in {:intent :status-of-queues
                         :to own-puk
                         :follower peer-puk
                         :highest-sequence-delivered -1
                         :highest-sequence-to-send -1
                         :full? false})
        (<!!? (async/filter< :reset packets-out)) => {:intent :send :from own-puk :to peer-puk :payload t1 :sequence 1 :reset true}
        (async/close! tuples-in)))
     
 (fact
   "it retries to send tuple" 
   (let [retry-timeout (chan 10)]
     (with-redefs [new-retry-timeout (constantly retry-timeout)]
       (let [tuples-in (dropping-chan)
             packets-in (dropping-chan)
             packets-out (chan)
             subject (start-queue-transmitter own-puk peer-puk (empty-store) tuples-in packets-in packets-out)]
         (>!! tuples-in t0)
         (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t0 :sequence 0}
         (>!! retry-timeout ::stimulus)
         (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t0 :sequence 0}
         (async/close! tuples-in))))))
