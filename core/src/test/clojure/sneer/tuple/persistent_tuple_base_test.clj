(ns sneer.tuple.persistent-tuple-base-test
  (:require [sneer.tuple.persistent-tuple-base :refer [query-all query-tuples store-tuple create]]
            [sneer.core :as core]            
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async :refer [chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.tuple.keys :refer [->puk]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(def neide (->puk "neide"))
(def carla (->puk "carla"))
(def michael (->puk "michael"))

(def t1 {"type" "tweet" "payload" "hi!" "author" neide})
(def t2 {"type" "tweet" "payload" "<3" "author" neide})

(defn- selecting [keys tuples]
  (map #(select-keys % keys) tuples))

(def select-ids
  (partial selecting ["id" "original_id"]))

(facts "About store-tuple"
  (with-open [db (jdbc-database/create-sqlite-db)]
    (let [subject (create db)]

      (fact "It remembers original-id"
        (let [t (assoc t1 "id" 42)]
          (store-tuple subject t)
          (->> (<!!? (query-all subject {"type" "tweet"})) select-ids) => [{"id" 1 "original_id" 42}]))

      (fact "It discards author/id duplicates"
        (let [duplicate {"type" "whatever" "author" carla "id" 42}
              unique (assoc duplicate "id" 43)]
          (store-tuple subject duplicate)
          (store-tuple subject duplicate)
          (store-tuple subject unique)
          (->> (<!!? (query-all subject {"type" "whatever"})) select-ids) => [{"id" 2 "original_id" 42} {"id" 3 "original_id" 43}])))))

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
          tuples [{"author" neide   "payload" "n"           "audience" carla}
                  {"author" carla   "payload" "c"           "audience" neide}
                  {"author" carla   "payload" "c"           "audience" neide}
                  {"author" michael "payload" "hello neide" "audience" neide "type" "chat"}
                  {"author" michael "payload" "hello carla" "audience" carla "type" "chat"}]]
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
  {"payload" "c"}                                    {"payload" "c"}
  {"author" neide "audience" carla}                  {"payload" "n"}
  {"author" carla "audience" neide}                  {"payload" "c"}
  {"author" neide "audience" carla "payload" "n"}    {"payload" "n"}
  {"author" michael "audience" neide}                {"payload" "hello neide"}
  {"author" michael "audience" carla}                {"payload" "hello carla"})
