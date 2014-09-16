(ns sneer.core.tests.reliable-client-test
  (:require [midje.sweet :refer :all]))

(defn create []
  {:sequence 0
   :payloads clojure.lang.PersistentQueue/EMPTY})

(defn enqueue-to-send [payload state]
  (update-in state [:payloads] conj payload))

(defn handle-packet-from-server [packet state]
  state)

(defn peek-packet [state]
  (when-some [payload (-> state :payloads first)]
    {:sequence (state :sequence)
     :payload payload}))

(defn pop-packet [state]
  (-> state
    (update-in [:payloads] pop)
    (update-in [:sequence] inc)))


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

