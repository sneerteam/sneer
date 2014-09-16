(ns sneer.core.tests.reliable-client-test
  (:require [midje.sweet :refer :all]
            [clojure.core.match :refer [match]]))

(defn create []
  {:sequence 0
   :payloads clojure.lang.PersistentQueue/EMPTY})

(defn enqueue-to-send [payload state]
  (update-in state [:payloads] conj payload))

(defn- pop-packet [state]
  (-> state
    (update-in [:payloads] pop)
    (update-in [:sequence] inc)))

(defn handle-packet-from-server [packet state]
  (match packet
         {:highest-sequence-to-send highest-sequence-to-send}
           (case (- (state :sequence) highest-sequence-to-send)
             0 (pop-packet state)
             1 state
             (assoc state :reset true))))

(defn peek-packet [state]
  (when-some [payload (-> state :payloads first)]
    (assoc (select-keys state [:sequence :reset]) :payload payload)))


(facts
 "New Queues"

  (fact "A new queue has no packet to send."
    (-> (create) peek-packet) => nil)

  (fact "Packet enqueing is FIFO."
    (let [queue (->> (create) (enqueue-to-send :foo) (enqueue-to-send :bar))]
      (->> queue peek-packet) => {:sequence 0 :payload :foo}
      (->> queue pop-packet peek-packet) => {:sequence 1 :payload :bar}))
  
  (fact "Every new packet gets a new sequence number."
  (let [queue (->> (create) (enqueue-to-send :foo))]
    (->> queue pop-packet (enqueue-to-send :bar) peek-packet :sequence) => 1)))


(facts
 "Packet Handling"

  (fact "Packet enqueing is FIFO."
    (let [queue (->> (create) (enqueue-to-send :foo) (enqueue-to-send :bar))
          simulate (fn [highest-sequence-to-send]
                     (->> queue
                       (handle-packet-from-server
                         {:intent :status-of-queues
                          :highest-sequence-delivered -1
                          :highest-sequence-to-send highest-sequence-to-send
                          :full? false})
                       peek-packet))]
    
    (simulate -1) => {:sequence 0 :payload :foo}
    (simulate 0)  => {:sequence 1 :payload :bar}
    (simulate 42) => {:sequence 0 :payload :foo :reset true})))

