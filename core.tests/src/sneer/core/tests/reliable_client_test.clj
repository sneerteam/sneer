(ns sneer.core.tests.reliable-client-test
  (:require [midje.sweet :refer :all]))

(defn create []
  clojure.lang.PersistentQueue/EMPTY)

(defn enqueue-to-send [packet state]
  (conj state packet))

(defn handle-packet [packet state]
  state)

(defn peek-packet-to-send [state]
  (first state))

(defn pop-packet-to-send [state]
  (pop state))


(facts
 "New Queues"

  (fact "A new queue has no packet to send."
    (-> (create) next-packet-to-send) => nil)

  (fact "First packet enqueue is first to be sent."
    (let [queue (create)]
      (->> queue (enqueue-to-send :foo) (peek-packet-to-send)) => :foo))

  (fact "First packet enqueue is first to be sent."
    (let [queue (create)]
      (->> queue (enqueue-to-send :foo) pop-packet-to-send peek-packet-to-send) => nil)))
