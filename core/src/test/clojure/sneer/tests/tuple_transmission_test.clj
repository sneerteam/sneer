(ns sneer.tests.tuple-transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple-transmission :refer [start-queue-transmitter QueueStore]]
            [clojure.core.async :as async :refer [chan]]))

(defn dropping-chan []
  (chan (async/dropping-buffer 1)))

(defn persistent-queue [& [elements]]
  (reduce conj clojure.lang.PersistentQueue/EMPTY elements))

(defn store-with-tuples [& tuples]
  (let [queue (atom (persistent-queue tuples))]
    (reify QueueStore
      (-peek [store to]
        (peek @queue)))))

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
        store (store-with-tuples {:sequence 42 :tuple t1})
        subject (start-queue-transmitter own-puk peer-puk store tuples-in packets-in packets-out)]
    (<!!? packets-out) => {:intent :send :from own-puk :to peer-puk :payload t1 :sequence 42}))

 (fact
  "when ack is received tuple is deleted from store ")

 )
