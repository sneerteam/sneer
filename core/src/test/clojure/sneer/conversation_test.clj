(ns sneer.conversation-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :as async]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as base]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.keys :refer [->puk]]
            [sneer.party :refer [reify-party]]
            [sneer.conversation :refer [reify-conversation]])
  (:import [sneer Conversation Party]
           [sneer.tuples TupleSpace]
           [rx Observable]
           [rx.subjects PublishSubject]))

; (do (require 'midje.repl) (midje.repl/autotest))

(def neide (->puk "neide"))

(def carla (->puk "carla"))

(facts "About reify-conversation"

  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (base/create db)]
    (let [^TupleSpace   tuple-space (space/reify-tuple-space neide tuple-base)
          ^Observable   menu-items (PublishSubject/create)
          ^Party        carla-party (reify-party carla)
          ^Conversation subject (reify-conversation tuple-space menu-items neide carla-party)
          most-recent-timestamps (observable->chan (subscribe-on-io (.mostRecentMessageTimestamp subject)))
          timestamp (long 42)
          message {"type" "message" "author" carla "audience" neide "timestamp" timestamp}]

      (fact "mostRecentMessageTimestamp includes message received"
        (base/store-tuple tuple-base message)
        (<!!? most-recent-timestamps) => timestamp)

      (fact "mostRecentMessageTimestamp includes message sent"
        (.sendMessage subject "hi")
        (<!!? most-recent-timestamps) => #(> % timestamp)))))
