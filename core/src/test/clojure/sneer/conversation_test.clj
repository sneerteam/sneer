(ns sneer.conversation-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.contact :refer [produce-contact create-contacts-state]]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as base]
            [sneer.test-util :refer [<!!? ->chan]]
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
  (let [scenario (neide-maicon-conversation-scenario!)]
    (with-open [_db (:db scenario)
                _tb (:tb scenario)]
      (let [n->m (:neide->maico scenario)
            m->n (:maico->neide scenario)

            most-recent-timestamps (->chan (.mostRecentMessageTimestamp m->n))
            most-recent-labels     (->chan (.mostRecentMessageContent   m->n))
            unread-counts          (->chan (.unreadMessageCount         m->n))
            messages               (->chan (.items                      m->n))

            t0 (System/currentTimeMillis)]

        (fact "mostRecentMessage includes message received"
          (<!!? unread-counts) => 0

          (.sendMessage n->m "Hi, Maico")
          (<!!? most-recent-timestamps) => :nil             ; Skipping history replay. :(
          (<!!? most-recent-timestamps) => :nil             ; Skipping history replay. :(
          (<!!? most-recent-timestamps) => #(>= % t0)

          (<!!? most-recent-labels) => :nil                 ; Skipping history replay. :(
          (<!!? most-recent-labels) => :nil                 ; Skipping history replay. :(
          (<!!? most-recent-labels) => "Hi, Maico"

          (<!!? unread-counts) => 0                         ; Skipping history replay. :(
          (<!!? unread-counts) => 1

          (<!!? messages) => []                             ; Skipping history replay. :(
          (<!!? messages) => []                             ; Skipping history replay. :(
          (let [msg (last (<!!? messages))]
            (.setRead m->n msg))
          (<!!? unread-counts) => 0)

        (fact "mostRecentMessageTimestamp includes message sent"
          (.sendMessage m->n "Hello, Neide")
          (<!!? most-recent-timestamps) => #(>= % t0)

          (<!!? most-recent-labels) => "Hello, Neide")))))
