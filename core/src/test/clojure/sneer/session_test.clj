(ns sneer.session-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple-base-provider :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.keys :refer [->puk]]
            [sneer.tuple.persistent-tuple-base :as tuple-base])
  (:import (sneer.crypto.impl KeysImpl)))

(defn- ->chan [obs]
  (observable->chan (subscribe-on-io obs)))

(facts "About Sessions"
  (with-open [db (create-sqlite-db)
              base (tuple-base/create db)
              neide-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) base)
              maico-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) base)]
    (let [neide-puk (.. neide-admin privateKey publicKey)
          maico-puk (.. maico-admin privateKey publicKey)
          neide-sneer (.sneer neide-admin)
          maico-sneer (.sneer maico-admin)
          neide (.produceContact maico-sneer "neide" (.produceParty maico-sneer neide-puk) nil)
          maico (.produceContact neide-sneer "maico" (.produceParty neide-sneer maico-puk) nil)
          n->m (.. neide-sneer conversations (withContact maico))
          m->n (.. maico-sneer conversations (withContact neide))]

        (fact "Unread message count starts at zero"
          (<!!? unread-n) => 0
          (<!!? unread-m) => 0)

        (fact "Neide sees her own messages in the session"
          (let [session (<!!? (->chan (.startSession n->m)))
                messages (->chan (.messages session))]
            (.send session "some payload")
            (.payload (<!!? messages)) => "some payload"))

        (fact "Maico sees this session"
          (let [sessions (->chan (.sessions m->n))
                replay1 (<!!? sessions)                 ; Skip history replay :(
                replay2 (<!!? sessions)                 ; Skip history replay :(
                session (first (<!!? sessions))
                messages (->chan (.messages session))]
            replay1 => []
            replay2 => []
            (.payload (<!!? messages)) => "some payload"
            (<!!? unread-m) => 0                        ; Skip history replay :(
            (<!!? unread-m) => 1
            (.send session "some reply")))

        (fact "Neide sees reply from Maico"
          (let [sessions (->chan (.sessions n->m))
                replay1 (<!!? sessions)                 ; Skip history replay :(
                replay2 (<!!? sessions)                 ; Skip history replay :(
                session (first (<!!? sessions))
                messages (->chan (.messages session))]
            replay1 => []
            replay2 => []
            (.payload (<!!? messages)) => "some payload"
            (.payload (<!!? messages)) => "some reply")
          (<!!? unread-n) => 0                          ; Skip history replay :(
          ; (<!!? unread-n) => 1
          )))))
