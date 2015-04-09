(ns sneer.tuple.tuple-transmitter-test
  (:require [sneer.tuple.tuple-transmitter :as tuple-transmitter]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.keys :refer [->puk]]
            [midje.sweet :refer :all]
            [clojure.core.async :refer [chan go >! close!]]
            [sneer.test-util :refer [>!!? <!!?]]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
            [sneer.tuple.protocols :refer [store-tuple restarted]])
  (:import (java.lang AutoCloseable)))

; (do (require 'midje.repl) (midje.repl/autotest))

(def A (->puk "neide"))
(def B (->puk "carla"))
(def C (->puk "michael"))

(defn get-transmitter-state []
  (let [tuples-in (chan)
        follower-connections (chan)
        connect-to-follower (fn [follower-puk tuples-out]
                              (go (>! follower-connections {follower-puk tuples-out})))
        tuples-for! (fn [follower-puk]
                      (get (<!!? follower-connections 500) follower-puk))]
    {:tuples-in tuples-in
     :connect-to-follower connect-to-follower
     :tuples-for! tuples-for!}))

(facts "About tuple transmitter"
  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (tuple-base/create db)]
    (let [{:keys [tuples-in connect-to-follower tuples-for!]} (get-transmitter-state)]

      (tuple-transmitter/start A tuple-base tuples-in connect-to-follower)

      (fact "It satisfies subs from stored tuples"
        (>!!? tuples-in {"type" "sub" "author" B "audience" A "criteria" {"type" "tweet"}})
        (let [tweet {"type" "tweet" "author" A "payload" "<3"}]
          (store-tuple tuple-base tweet)
          (let [tuples-for-b (tuples-for! B)
                [tuple ack-chan] (<!!? tuples-for-b)]
            tuple => (contains tweet)
            (>!!? ack-chan tuple))))

      (fact "It sends subs to followees"
        (let [sub-sent {"type" "sub" "author" A "criteria" {"type" "tweet" "author" C}}]
          (store-tuple tuple-base sub-sent)
          (let [tuples-for-c (tuples-for! C)
                [sub-recv _] (<!!? tuples-for-c)]
            sub-recv => (contains (assoc sub-sent "audience" C)))))

      (fact "It wont resend tuples upon restart"
        (close! tuples-in)
        (with-open [^AutoCloseable tuple-base (restarted tuple-base)]
          (let [tuples-in (chan)]
            (tuple-transmitter/start A tuple-base tuples-in connect-to-follower)

            (let [new-tweet {"type" "tweet" "author" A "payload" "S2"}]
              (store-tuple tuple-base new-tweet)
              (let [tuples-for-b (tuples-for! B)
                    _ (assert tuples-for-b "tuples-for-b second time")
                    [tuple _] (<!!? tuples-for-b)]
                tuple => (contains new-tweet)))))))))

(facts "About pushes"
  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (tuple-base/create db)]
    (let [{:keys [tuples-in connect-to-follower tuples-for!]} (get-transmitter-state)]

      (tuple-transmitter/start A tuple-base tuples-in connect-to-follower)

      (fact "It sends pushes"
        (let [push-sent {"type" "push" "author" A "value" 42 "audience" C}]
          (store-tuple tuple-base push-sent)
          (let [tuples-for-c (tuples-for! C)
                [push-recv _] (<!!? tuples-for-c)]
            push-recv => (contains push-sent)))))))