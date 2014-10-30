(ns sneer.tests.tuple-transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple-transmission :refer [start-queue-transmitter QueueStore new-retry-timeout]]
            [sneer.async :refer :all]
            [clojure.core.async :as async :refer [chan >!! <! go-loop]]))

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
    (async/timeout 200) :timeout
    ch ([v] v)))
         
(defn >!!? [ch v]
  (async/alt!!
    (async/timeout 200) false
    [[ch v]] true))

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
      (let [tuples-in (chan)
            packets-in (dropping-chan 2)
            packets-out (sliding-chan)
            store (empty-store)
            subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
        (>!!? tuples-in t0)
        (>!!? tuples-in t1) => true      
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

(defn wait-for-last [ch]
  (go-loop [previous nil]
    (if-some [v (<! ch)]
      (recur v)
      previous)))

(tabular "Packet Handling"
   (fact 
     (let [tuples-in (chan)
           packets-in (chan)
           packets-out (sliding-chan)
           enqueue (fn [start count]
                     (doall (map #(>!!? tuples-in {:tag %}) (range start (+ start count)))))
           simulate-from-server (fn [highest-sequence-to-send highest-sequence-delivered full?]
                                  (when highest-sequence-to-send
                                    (>!!? packets-in
                                      {:intent :status-of-queues
                                       :highest-sequence-to-send highest-sequence-to-send
                                       :highest-sequence-delivered highest-sequence-delivered
                                       :full? full?})))]
       (start-queue-transmitter own-puk peer-puk (empty-store) tuples-in packets-in packets-out)
       (enqueue     0 ?enq1)
       (simulate-from-server ?hsts1 ?hsd1 ?full?1)
       (enqueue ?enq1 ?enq2)
       (simulate-from-server ?hsts2 ?hsd2 ?full?2)
       (async/close! tuples-in)

       (let [packet-to-send (<!!? (wait-for-last packets-out))]
         (println ?seq packet-to-send)
    
         (:sequence packet-to-send) => ?seq
         (-> packet-to-send :payload :tag) => ?seq ;This test is designed so that the sequence and the payload are always the same.
         (:reset packet-to-send) => ?reset)))
     
  
     ?enq1 ?hsts1 ?hsd1 ?full?1 ?enq2 ?hsts2 ?hsd2 ?full?2   ?seq ?reset   ?obs
         0    nil   nil     nil     0    nil   nil     nil    nil    nil   "A new queue has no packet to send."
         1    nil   nil     nil     0    nil   nil     nil      0    nil   "A packet can be enqueued to send."
;         2    nil   nil     nil     0    nil   nil     nil      0    nil   "Enqueueing is FIFO."
;         1     -1    -1   false     0    nil   nil     nil      0    nil   "When the server has no packets sent (initial server state), queue sends first packet."
;         1      0    -1   false     0    nil   nil     nil    nil    nil   "Server sending a packet pops it from the queue (with one enqueued)."
;         3      0    -1   false     0      1    -1   false      2    nil   "Server sending a packet pops it from the queue (with three enqueued)."
;         1     42     0   false     0    nil   nil     nil      0   true   "Reset is sent when server gets out of sync."
;         2     42     0   false     0     -1    -1   false      0    nil   "Reset is not needed for happy-day sequencing."
;         1      0    -1   false     0     -1    -1   false      0   true   "Undelivered packets are sent when the server restarts."
;         1      0     0   false     0     -1    -1   false    nil    nil   "Delivered packets are forgotten (with one enqueued)."
;         2      0     0   false     0     -1    -1   false      1   true   "Delivered packets are forgotten (with two enqueued)."
;         7      0     0   false     0     -1    -1   false      1   true   "Delivered packets are forgotten (with several enqueued)."
)
