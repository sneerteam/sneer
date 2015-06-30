(ns sneer.tuple.persistent-tuple-base-test
  (:require [sneer.tuple.persistent-tuple-base :refer :all]
            [sneer.tuple.protocols :refer :all]
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async :refer [chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.keys :refer [->puk]]))

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
  (with-open [db (jdbc-database/create-sqlite-db)
              subject (create db)]

    (fact "It remembers original-id"
      (let [t (assoc t1 "id" 42)]
        (let [stored-tuple-chan (store-tuple subject t)]
          (fact "It returns channel that emits stored tuple"
            (<!!? stored-tuple-chan) => (contains {"id" 1})))
        (->> (<!!? (query-all subject {"type" "tweet"})) select-ids) => [{"id" 1 "original_id" 42}]))

    (fact "It discards author/id duplicates"
      (let [duplicate {"type" "whatever" "author" carla "id" 42}
            unique (assoc duplicate "id" 43)]
        (store-tuple subject duplicate)
        (store-tuple subject duplicate)
        (store-tuple subject unique)
        (select-ids (<!!? (query-all subject {"type" "whatever"}))) => [{"id" 2 "original_id" 42}
                                                                        {"id" 3 "original_id" 43}]))

    (fact "It accepts an optional uniqueness criteria"
      (let [t {"type" "unique" "author" neide}
            query-unique #(->> (<!!? (query-all subject t)) (selecting ["type"]))]
        (store-tuple subject t t)
        (query-unique) => [{"type" "unique"}]
        (store-tuple subject t t)
        (query-unique) => [{"type" "unique"}]))))

(facts "About query-tuples"
  (with-open [db (jdbc-database/create-sqlite-db)
              subject (create db)]
    (let [result (async/chan)
          lease (async/chan)
          _ (<!!? (store-tuple subject t1))
          query (query-tuples subject {"type" "tweet"} result lease)]

      (fact "It sends stored tuples"
        (<!!? result) => (contains t1))

      (fact "When query is live it sends new tuples"
        (store-tuple subject t2)
        (<!!? result) => (contains t2))

      (fact "When lease channel is closed query-tuples is terminated"
        (async/close! lease)
        (<!!? query) => nil))))

(facts "About tuple query with history"
  (with-open [db (jdbc-database/create-sqlite-db)
              subject (create db)]
    (let [old-tuples (async/chan)
          new-tuples (async/chan)
          lease (async/chan)
          _ (<!!? (store-tuple subject t1))
          query (query-with-history subject {"type" "tweet"} old-tuples new-tuples lease)]

      (fact "It sends old stored tuples"
        (<!!? old-tuples) => (contains t1))

      (fact "It closes the tuple history channel"
        (<!!? old-tuples) => nil)

      (fact "It sends new tuples"
        (store-tuple subject t2)
        (<!!? new-tuples) => (contains t2))

      (fact "When lease channel is closed query is terminated"
        (async/close! lease)
        (<!!? query) => nil))))

(tabular "About query criteria"
  (with-open [db (jdbc-database/create-sqlite-db)
              subject (create db)]
    (let [result (async/chan)
          tuples [{"author" neide   "payload" "n"           "audience" carla "id" 42}
                  {"author" carla   "payload" "c"           "audience" neide}
                  {"author" carla   "payload" "c"           "audience" neide}
                  {"author" michael "payload" "hello neide" "audience" neide}
                  {"author" michael "payload" "hello carla" "audience" carla "custom-field" "urgent"}]]
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
  {"author" michael "audience" carla}                {"payload" "hello carla"}
  {"original_id" 42}                                 {"payload" "n" "original_id" 42}
  {"custom-field" "urgent"}                          {"payload" "hello carla"})

(facts "About :last-by-id query criterion"
  (with-open [db (jdbc-database/create-sqlite-db)
              subject (create db)]

    (doseq [tuple [t1 t2]]
      (store-tuple subject tuple))

    (let [criteria {"type" "tweet"
                    last-by-id true}]
      (fact "given truthy will emit from the last tuple on"
        (->>
         (<!!? (query-all subject criteria))
         (selecting ["payload"])) => [(select-keys t2 ["payload"])])

      (fact "live queries honor :last-by-id"
        (let [lease (chan)
              result (chan)
              t3 (assoc t2 "payload" "t3")]
          (query-tuples subject criteria result lease)
          (<!!? result) => (contains t2)
          (store-tuple subject t3)
          (<!!? result) => (contains t3))))))

(facts "About tuple attributes"
  (with-open [db (jdbc-database/create-sqlite-db)
              subject (create db)]
    (let [tuple {"type" "test" "author" neide}
          tuple-response (chan)]
      (store-tuple subject tuple)
      (query-tuples subject tuple tuple-response)
      (let [tuple-id (get (<!!? tuple-response) "id")
            attr-response  (chan)]

        (fact "Absent value is :null"
          (get-local-attribute subject :some-tag :null tuple-id attr-response)
          (<!!? attr-response) => :null)

        (fact "value can be set"
          (set-local-attribute subject :some-tag 42 tuple-id)
          (get-local-attribute subject :some-tag :null tuple-id attr-response)
          (<!!? attr-response) => 42)

        (fact "value can be updated"
          (set-local-attribute subject :some-tag "foo" tuple-id)
          (get-local-attribute subject :some-tag :null tuple-id attr-response)
          (<!!? attr-response) => "foo")))))
