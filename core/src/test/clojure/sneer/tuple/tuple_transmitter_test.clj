(ns sneer.tuple.tuple-transmitter-test
  (:require [sneer.tuple.tuple-transmitter :as tuple-transmitter]
            [sneer.tuple.jdbc-tuple-base :as jdbc-tuple-base]
            [sneer.tuple.keys :refer [->puk]]
            [midje.sweet :refer :all]
            [clojure.core.async :refer [chan]]
            [sneer.test-util :refer [>!!? <!!?]]
            [sneer.tuple.persistent-tuple-base :refer [store-tuple]]))

(def A (->puk "neide"))
(def B (->puk "carla"))
(def C (->puk "michael"))

(facts "About tuple transmitter"
  (let [tuple-base (jdbc-tuple-base/create)
        follower-connections (atom {})
        connect-to-follower (fn [follower-puk tuples-out] (swap! follower-connections assoc follower-puk tuples-out))
        tuples-in (chan)
        subject (tuple-transmitter/start A tuple-base tuples-in connect-to-follower)]
    (fact "It satisfies subs from stored tuples"
      (>!!? tuples-in {"type" "sub" "author" B "audience" A "criteria" {"type" "tweet"}})
      (let [tweet {"type" "tweet" "author" A "payload" "<3"}]
        (store-tuple tuple-base tweet)
        (<!!? (get @follower-connections B)) => tweet))))
