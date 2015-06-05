(ns sneer.conversation-test
  (:require [clojure.core.async :refer [chan <! >! pipe]]
            [midje.sweet :refer :all]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.contact :refer [produce-contact create-contacts-state]]
            [sneer.tuple.persistent-tuple-base :as tb]
            [sneer.tuple.tuple-transmitter :as transmitter]
            [sneer.test-util :refer [<!!? ->chan]]
            [sneer.tuple.protocols :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.keys :refer [->puk create-prik]]
            [sneer.party :refer [reify-party create-puk->party]]
            [sneer.conversation :refer [reify-conversation]]
            [sneer.async :refer [go-while-let]])
  (:import [sneer.crypto.impl KeysImpl]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn neide-maico-conversation-scenario! []
  (let [neide-db (create-sqlite-db)
        maico-db (create-sqlite-db)
        neide-tb (tb/create neide-db)
        maico-tb (tb/create maico-db)
        neide-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) neide-tb)
        maico-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) maico-tb)
        neide-puk (.. neide-admin privateKey publicKey)
        maico-puk (.. maico-admin privateKey publicKey)
        neide-sneer (.sneer neide-admin)
        maico-sneer (.sneer maico-admin)
        neide-received (chan)
        maico-received (chan)

        _ (transmitter/start neide-puk neide-tb neide-received
                             (fn [follower-puk tuples-out & _]
                               (assert (= follower-puk maico-puk))
                               (go-while-let [[tuple ack-ch] (<! tuples-out)]
                                 (>! maico-received tuple)
                                 (>! ack-ch tuple))))
        _ (transmitter/start maico-puk maico-tb maico-received
                             (fn [follower-puk tuples-out & _]
                               (assert (= follower-puk neide-puk))
                               (go-while-let [[tuple ack-ch] (<! tuples-out)]
                                 (>! neide-received tuple)
                                 (>! ack-ch tuple))))
        neide (.produceContact maico-sneer "neide" (.produceParty maico-sneer neide-puk) nil)
        maico (.produceContact neide-sneer "maico" (.produceParty neide-sneer maico-puk) nil)]
    {:neide-db neide-db
     :maico-db maico-db
     :neide-tb neide-tb
     :maico-tb maico-tb
     :neide->maico (.. neide-sneer conversations (withContact maico))
     :maico->neide (.. maico-sneer conversations (withContact neide))}))


(def neide (->puk "neide"))

(facts "About reify-conversation"
  (let [scenario (neide-maico-conversation-scenario!)]
    (with-open [_neide-db (:neide-db scenario)
                _maico-db (:maico-db scenario)
                _neide-tb (:neide-tb scenario)
                _maico-tb (:maico-tb scenario)]
      (let [n->m (:neide->maico scenario)
            m->n (:maico->neide scenario)

            items                  (->chan (.items                      m->n))
            most-recent-labels     (->chan (.mostRecentMessageContent   m->n))
            most-recent-timestamps (->chan (.mostRecentMessageTimestamp m->n))
            unread-counts          (->chan (.unreadMessageCount         m->n))

            t0 (System/currentTimeMillis)]

        (fact "initial values are empty"
          (<!!? items) => []
          (<!!? most-recent-labels) => :nil
          (<!!? most-recent-timestamps) => :nil
          (<!!? unread-counts) => 0)

        (fact "new message received increments values"
          (.sendMessage n->m "Hi, Maico")
          (<!!? unread-counts) => 0                         ; Skipping history replay. :(
          (<!!? unread-counts) => 1

          (<!!? most-recent-timestamps) => :nil             ; Skipping history replay. :(
          (<!!? most-recent-timestamps) => #(>= % t0)

          (<!!? most-recent-labels) => :nil                 ; Skipping history replay. :(
          (<!!? most-recent-labels) => "Hi, Maico"

          (.sendMessage n->m "Hi, Maico2")
          (<!!? most-recent-labels) => "Hi, Maico2"
          (<!!? unread-counts) => 2

          (.sendMessage n->m "Hi, Maico3")
          (<!!? most-recent-labels) => "Hi, Maico3"
          (<!!? unread-counts) => 3

          (<!!? items) => []                             ; Skipping history replay. :(
          (let [msg (first (<!!? items))]
            (.setRead m->n msg))
          (<!!? unread-counts) => 2)

        (fact "mostRecentLabel includes message sent"
          (.sendMessage m->n "Hello, Neide")
          (<!!? unread-counts) => 2                         ; Sending messages re-emits the unread-count. :(
          (<!!? most-recent-timestamps) => #(>= % t0)
          (<!!? most-recent-labels) => "Hello, Neide")))))
