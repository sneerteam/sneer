(ns sneer.conversation-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :as async]
            [sneer.contact :refer [produce-contact create-contacts-state]]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as base]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.tuple.protocols :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.keys :refer [->puk create-prik]]
            [sneer.party :refer [reify-party create-puk->party]]
            [sneer.conversation :refer [reify-conversation]])
  (:import [sneer Conversation Party Contact]
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
          own-prik (create-prik)
          own-puk (.publicKey own-prik)
          puk->party (create-puk->party)
          contacts-state (create-contacts-state tuple-space own-puk puk->party)
          ^Observable   menu-items (PublishSubject/create)
          ^Party        carla-party (reify-party carla)
          ^Contact      carla-contact (produce-contact contacts-state "carla" carla-party nil)
          ^Conversation subject (reify-conversation tuple-space menu-items neide carla-contact)
          most-recent-timestamps (observable->chan (subscribe-on-io (.mostRecentMessageTimestamp subject)))
          timestamp (long 42)
          message {"type" "message" "author" carla "audience" neide "timestamp" timestamp}]

      (fact "mostRecentMessageTimestamp includes message received"
        (store-tuple tuple-base message)
        (<!!? most-recent-timestamps) => timestamp)

      (fact "mostRecentMessageTimestamp includes message sent"
        (.sendMessage subject "hi")
        (<!!? most-recent-timestamps) => #(> % timestamp)))))
