(ns sneer.core.tests.reliable-client-test
  (:require [midje.sweet :refer :all]))

(defn create []
  clojure.lang.PersistentQueue/EMPTY)

(defn enqueue-to-send [payload state]
  (conj state {:payload payload :sequence (count state)}))

(defn handle-packet-from-server [packet state]
  state)

(defn peek-packet [state]
  (first state))

(defn pop-packet [state]
  (pop state))


(facts
 "New Queues"

  (fact "A new queue has no packet to send."
    (-> (create) peek-packet) => nil)

  (fact "Packet enqueing is FIFO."
    (let [queue (->> (create) (enqueue-to-send :foo) (enqueue-to-send :bar))]
      (->> queue peek-packet) => {:sequence 0 :payload :foo}
      (->> queue pop-packet peek-packet) => {:sequence 1 :payload :bar})))

