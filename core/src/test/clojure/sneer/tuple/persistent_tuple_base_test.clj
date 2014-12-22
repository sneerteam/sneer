(ns sneer.tuple.persistent-tuple-base-test
  (:require [sneer.tuple.persistent-tuple-base :refer [query-tuples store-tuple create]]
            [sneer.core :as core]            
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.tuple.keys :refer [->puk]]))

;  (do (require 'midje.repl) (midje.repl/autotest))

(def neide (->puk "neide"))
(def carla (->puk "carla"))

(def t1 {"type" "tweet" "payload" "hi!" "author" neide})
(def t2 {"type" "tweet" "payload" "<3" "author" neide})

(facts "About query-tuples"
  (with-open [db (jdbc-database/create-sqlite-db)]
    (let [subject (create db)
          result (async/chan)
          lease (async/chan)
          _ (store-tuple subject t1)
          query (query-tuples subject {"type" "tweet"} result lease)]

      (fact "It sends stored tuples"
        (<!!? result) => (contains t1))

      (fact "When query is live it sends new tuples"
        (store-tuple subject t2)
        (<!!? result) => (contains t2))

      (fact "When lease channel is closed query-tuples is terminated"
        (async/close! lease)
        (<!!? query) => nil))))

(tabular "About query criteria"
  (with-open [db (jdbc-database/create-sqlite-db)]
    (let [subject (create db)
          result (async/chan)
          tuples [{"author" neide "payload" "n" "audience" carla}
                  {"author" carla "payload" "c" "audience" neide}]]
      (doseq [tuple tuples]
        (store-tuple subject (assoc tuple "type" "test")))
      (query-tuples subject ?criteria result)
      (fact
          (<!!? result) => (contains ?expected))))

  ?criteria                                          ?expected
  {"author" neide}                                   {"payload" "n"}
  {"author" carla}                                   {"payload" "c"}
  {"audience" carla}                                 {"payload" "n"}
  {"audience" neide}                                 {"payload" "c"}
  {"payload" "n"}                                    {"payload" "n"}
  {"payload" "c"}     	                             {"payload" "c"}
  {"author" neide "audience" carla}                  {"payload" "n"}
  {"author" carla "audience" neide}                  {"payload" "c"}
  {"author" neide "audience" carla "payload" "n"}    {"payload" "n"}
  {"author" carla "audience" neide "payload" "c"}    {"payload" "c"})
