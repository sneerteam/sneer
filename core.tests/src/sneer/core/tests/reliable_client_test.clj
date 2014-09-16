(ns sneer.core.tests.reliable-client-test
  (:require [midje.sweet :refer :all]))

(defn create []
  {})

(defn enqueue-to-send [packet state]
  [state])

(defn handle-packet [packet state]
  [state])

(defn next-packet-to-send [state]
  ;[packet])
  )

(facts
 "New Queues"

  (fact "A new queue has no packet to send."
       (-> create next-packet-to-send) => nil))
