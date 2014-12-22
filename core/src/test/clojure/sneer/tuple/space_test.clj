(ns sneer.tuple.space-test
  (:require [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as base]
            [sneer.core :as core]
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.tuple.keys :refer [->puk]]
            [rx.lang.clojure.core :as rx]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- subscribe-chan [observable c]
  (rx/subscribe observable
                #(async/>!! c %)
                #(do
                   (println "Rx Error:" %)
                   (async/close! c))
                #(async/close! c)))

(defn- ->chan [observable]
  (let [c (async/chan)]
    (subscribe-chan observable c)
    c))

(def neide (->puk "neide"))

(def t1 {"type" "tweet" "payload" "hi!" "author" neide})
(def t2 {"type" "tweet" "payload" "<3" "author" neide})

(facts "#rx-query-tuples"
  (with-open [db (jdbc-database/create-sqlite-db)]
    (let [tuple-base (base/create db)]

      (base/store-tuple tuple-base t1)
      
      (fact "When keep-alive is false"
        (let [observable-tuples (space/rx-query-tuples tuple-base {"type" "tweet"} false)
              tuples (->chan observable-tuples)]
          (<!!? tuples) => (contains t1)
          (<!!? tuples) => nil))

      (fact "When keep-alive is true"
        (let [observable-tuples (space/rx-query-tuples tuple-base {"type" "tweet"} true)
              tuples (->chan observable-tuples)]
          (<!!? tuples) => (contains t1)
          (base/store-tuple tuple-base t2)
          (<!!? tuples) => (contains t2)))

      (fact "When unsubscribe it closes the channel"
        (let [observable-tuples (space/rx-query-tuples tuple-base {"type" "tweet"} true)
              tuples (async/chan)
              subscriber (subscribe-chan observable-tuples tuples)]
          (.unsubscribe subscriber)
          (<!!? (async/filter< nil? tuples)) => nil))
      
      )))
