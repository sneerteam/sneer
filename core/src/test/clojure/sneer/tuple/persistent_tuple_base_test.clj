(ns sneer.tuple.persistent-tuple-base-test
  (:require [sneer.tuple.persistent-tuple-base :refer [query-tuples store-tuple create]]
            [sneer.core :as core]            
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.tuple.keys :refer [->puk]]))

;  (do (require 'midje.repl) (midje.repl/autotest))

(facts "About query-tuples"
  (with-open [db (jdbc-database/create-sqlite-db)]
    (let [subject (create db)
          result (async/chan)
          lease (async/chan)
          t1 {"type" "sub" "payload" "42" "author" (->puk "neide")}
          query (query-tuples subject {"type" "sub"} result lease)]

      (fact "When query is live it sends new tuples"
        (store-tuple subject t1)
        (<!!? result) => (contains t1))

      (fact "When lease channel is closed query-tuples is terminated"
        (async/close! lease)
        (<!!? query) => nil))))
