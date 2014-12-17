(ns sneer.tuple.persistent-tuple-base-test
  (:require [sneer.tuple.persistent-tuple-base :refer [query-tuples store-tuple]]
            [sneer.core :as core]            
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async]
            [sneer.tuple.jdbc-tuple-base :as jdbc-tuple-base]
            [sneer.tuple.keys :refer [->puk]]))

;  (do (require 'midje.repl) (midje.repl/autotest))

(facts "About query-tuples"
  (let [subject (jdbc-tuple-base/create)
        result (async/chan)
        lease (async/chan)
        t1 {"type" "sub" "payload" "42" "author" (->puk "neide")}
        select-t1-keys #(select-keys % (keys t1))
        query (query-tuples subject {"type" "sub"} result lease)]

    (fact "When query is live it sends new tuples"
      (store-tuple subject t1)
      (-> (<!!? result) select-t1-keys) => t1)

    (fact "When lease channel is closed query-tuples is terminated"
      (async/close! lease)
      (<!!? query) => nil)))
