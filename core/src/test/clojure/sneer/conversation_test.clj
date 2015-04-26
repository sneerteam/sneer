(ns sneer.conversation-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.contact :refer [produce-contact create-contacts-state]]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as base]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.tuple.protocols :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.keys :refer [->puk create-prik]]
            [sneer.party :refer [reify-party create-puk->party]]
            [sneer.conversation :refer [reify-conversation]]
            [sneer.tuple.persistent-tuple-base :as tuple-base])
  (:import [sneer Conversation Party Contact]
           [sneer.tuples TupleSpace]
           [sneer.crypto.impl KeysImpl]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn neide-maicon-conversation-scenario! []
  (let [db (create-sqlite-db)
        tb (tuple-base/create db)
        neide-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) tb)
        maico-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) tb)
        neide-puk (.. neide-admin privateKey publicKey)
        maico-puk (.. maico-admin privateKey publicKey)
        neide-sneer (.sneer neide-admin)
        maico-sneer (.sneer maico-admin)
        neide (.produceContact maico-sneer "neide" (.produceParty maico-sneer neide-puk) nil)
        maico (.produceContact neide-sneer "maico" (.produceParty neide-sneer maico-puk) nil)]
    {:db db
     :tb tb
     :neide->maico (.. neide-sneer conversations (withContact maico))
     :maico->neide (.. maico-sneer conversations (withContact neide))}))


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
          ^Party        carla-party (reify-party carla)
          ^Contact      carla-contact (produce-contact contacts-state "carla" carla-party nil)
          ^Conversation subject (reify-conversation tuple-space neide carla-contact)
          most-recent-timestamps (observable->chan (subscribe-on-io (.mostRecentMessageTimestamp subject)))
          most-recent-labels     (observable->chan (subscribe-on-io (.mostRecentMessageContent   subject)))
          unread-counts          (observable->chan (subscribe-on-io (.unreadMessageCount         subject)))
          messages               (observable->chan (subscribe-on-io (.items                      subject)))
          timestamp (System/currentTimeMillis)
          message {"type" "message" "author" carla "audience" neide "timestamp" timestamp "label" "Hi, Neide"}]

      (fact "mostRecentMessage includes message received"
        (<!!? unread-counts         ) => 0

        (store-tuple tuple-base message)
        (<!!? most-recent-timestamps) => :nil               ; Skipping history replay. :(
        (<!!? most-recent-timestamps) => :nil               ; Skipping history replay. :(
        (<!!? most-recent-timestamps) => timestamp

        (<!!? most-recent-labels    ) => :nil               ; Skipping history replay. :(
        (<!!? most-recent-labels    ) => :nil               ; Skipping history replay. :(
        (<!!? most-recent-labels    ) => "Hi, Neide"

        (<!!? unread-counts         ) => 0                  ; Skipping history replay. :(
        (<!!? unread-counts         ) => 1

        (<!!? messages              ) => []                 ; Skipping history replay. :(
        (<!!? messages              ) => []                 ; Skipping history replay. :(
        (let [msg (last (<!!? messages))]
          (.setRead subject msg))
        (<!!? unread-counts         ) => 0)

      (fact "mostRecentMessageTimestamp includes message sent"
        (.sendMessage subject "Hello, Carla")
        (<!!? most-recent-timestamps) => #(> % timestamp)

        (<!!? most-recent-labels    ) => "Hello, Carla"))))
