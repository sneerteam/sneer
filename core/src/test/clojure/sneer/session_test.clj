(ns sneer.session-test
  (:require [midje.sweet :refer :all]
            [sneer.rx :refer :all]

            [sneer.tuple-base-provider :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.test-util :refer [<!!?]]
            [sneer.rx-test-util :refer [->chan ->chan2 subscribe-chan]]
            [sneer.keys :refer [->puk]]
            [sneer.conversation-test :refer [neide-maico-conversation-scenario!]]))

(defn messages [session]
  (->chan2 (.messages session)
          (comp (filter #(not (.isUpToDate %)))
                (map #(.message %)))))

(try
  (facts "About Sessions"
    (let [scenario (neide-maico-conversation-scenario!)]
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
            (let [session (<!!? (->chan (.startSession n->m "some type")))
                  messages (messages session)]
              (.send session "some payload")
              (<!!? messages) => #(-> % .payload (= "some payload"))))

          (fact "Maico sees this session"
            (let [sessions (->chan (.sessions m->n))
                  replay1 (<!!? sessions)                   ; Skip history replay :(
                  replay2 (<!!? sessions)                   ; Skip history replay :(
                  session (first (<!!? sessions))
                  messages (messages session)]
              replay1 => []
              replay2 => []
              (.type session) => "some type"
              (<!!? messages 2000) => #(-> % .payload (= "some payload"))
              ;;(<!!? unread-m) => 0                        ; Skip history replay :(
              ;;(<!!? unread-m) => 1
              (.send session "some reply")))

          (fact "Neide sees reply from Maico"
            (let [sessions (->chan (.sessions n->m))
                  replay1 (<!!? sessions)                   ; Skip history replay :(
                  replay2 (<!!? sessions)                   ; Skip history replay :(
                  session (first (<!!? sessions))
                  messages (messages session)]
              replay1 => []
              replay2 => []
              (.payload (<!!? messages)) => "some payload"
              (.payload (<!!? messages)) => "some reply")
            ;;(<!!? unread-n) => 0                          ; Skip history replay :(
            ; (<!!? unread-n) => 1
            )))))

  (catch Exception e (.printStackTrace e)))
