(ns sneer.session-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple-base-provider :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.test-util :refer [<!!? ->chan]]
            [sneer.keys :refer [->puk]]
            [sneer.conversation-test :refer [neide-maicon-conversation-scenario!]]))

(facts "About Sessions"
  (let [scenario (neide-maicon-conversation-scenario!)]
    (with-open [_neide-db (:neide-db scenario)
                _maico-db (:maico-db scenario)
                _neide-tb (:neide-tb scenario)
                _maico-tb (:maico-tb scenario)]
      (let [n->m (:neide->maico scenario)
            m->n (:maico->neide scenario)
            unread-n (->chan (.unreadMessageCount n->m))
            unread-m (->chan (.unreadMessageCount m->n))]

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
            (<!!? messages) => #(-> % .payload (= "some payload"))
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
            (<!!? messages) => #(-> % .payload (= "some payload"))
            (<!!? messages) => #(-> % .payload (= "some reply")))
          (<!!? unread-n) => 0                          ; Skip history replay :(
          ; (<!!? unread-n) => 1
          )))))
