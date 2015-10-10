(ns sneer.tuple.space-test
  (:import [sneer.tuples TupleSpace TuplePublisher])
  (:require [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as base]
            [sneer.tuple.protocols :refer [store-tuple query-tuples]]
            [sneer.rx-test-util :refer [subscribe-chan observable->chan]]
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async :refer [>!]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.keys :refer [->puk]]
            [rx.lang.clojure.core :as rx]))

; (do (require 'midje.repl) (midje.repl/autotest))

(def neide (->puk "neide"))
(def carla (->puk "carla"))

(def t1 {"type" "tweet" "payload" "hi!" "author" neide})
(def t2 {"type" "tweet" "payload" "<3" "author" neide})

(facts "#rx-query-tuples"
  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (base/create db)]

    (store-tuple tuple-base t1)

    (fact "When keep-alive is false"
      (let [observable-tuples (space/rx-query-tuples tuple-base {"type" "tweet"} false)
            tuples (observable->chan observable-tuples)]
        (<!!? tuples) => (contains t1)
        (<!!? tuples) => nil))

    (fact "When keep-alive is true"
      (let [observable-tuples (space/rx-query-tuples tuple-base {"type" "tweet"} true)
            tuples (observable->chan observable-tuples)]
        (<!!? tuples) => (contains t1)
        (store-tuple tuple-base t2)
        (<!!? tuples) => (contains t2)))

    (fact "When unsubscribe it closes the channel"
      (let [observable-tuples (space/rx-query-tuples tuple-base {"type" "tweet"} true)
            tuples (async/chan)
            subscriber (subscribe-chan tuples observable-tuples)]
        (.unsubscribe subscriber)
        (<!!? (async/filter< nil? tuples)) => nil))))

(facts "About TuplePublisher#pub"
  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (base/create db)]
    (let [^TupleSpace     tuple-space (space/reify-tuple-space neide tuple-base)
          ^TuplePublisher subject (.publisher tuple-space)]

      (fact "Emits tuple with id after publishing"
        (let [tuple-out (async/chan)]

          (rx/subscribe
            (.. subject (type "has-id") (pub "42"))
            (fn [tuple] (async/go (>! tuple-out tuple))))

          (get (<!!? tuple-out) "id") => 1)))))


(facts "About TupleFilter#tuples"
  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (base/create db)]
    (let [subject (space/new-tuple-filter tuple-base neide)]

      (fact "Stores single tuple for similar sub"
        (let [filter (.. subject (author carla) tuples)
              filter2 (.. subject (author carla) (field "f" 42) tuples)
              tuples (async/chan)
              subscribers [(subscribe-chan tuples filter) (subscribe-chan tuples filter) (subscribe-chan tuples filter2)]
              subs (async/chan)]
          (query-tuples tuple-base {"type" "sub" "author" neide} subs)
          (<!!? subs) => (contains {"criteria" {"author" carla}})
          (<!!? subs) => (contains {"criteria" {"author" carla, "f" 42}})
          (<!!? subs) => nil
          (doseq [s subscribers] (.unsubscribe s)))))))
