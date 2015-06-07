(ns sneer.conversation-summarization-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!] :as async]
            [sneer.async :refer [sliding-chan]]
            [sneer.commons :refer [submap?]]
            [sneer.test-util :refer [<!!? <emits tmp-file]]
            [sneer.tuple.jdbc-database :as database]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
            [sneer.tuple.protocols :refer :all]
            [sneer.keys :as keys]
            [sneer.convos :as convos])
  (:import  [java.io File]))


(defn- summarize! [events expected-summaries]

  (with-open [db (database/create-sqlite-db)
              tuple-base (tuple-base/create db)]

    (let [own-puk (keys/->puk "neide puk")

          proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
          store-contact (fn [contact] (store-tuple tuple-base (merge proto-contact contact)))

          proto-message {"type" "message" "message-type" "chat"}
          store-message (fn [tuple] (store-tuple tuple-base (merge proto-message tuple)))

          label->msg (atom {})
          store-read (fn [contact-puk msg] (store-tuple tuple-base
                                                        {"author" own-puk "type" "message-read" "audience" contact-puk "payload" (msg "original_id")}))

          subject (atom nil)
          lease (atom nil)
          summaries-out (chan (async/sliding-buffer 1) (map #(mapv (fn [summary] (select-keys summary [:name :timestamp :preview :unread])) (convos/summarize %))))
          restart-subject (fn []
                            (swap! lease #(do
                                            (when % (do (close! %)
                                                        (<!!? @subject)))
                                            (chan)))
                            (reset! subject
                                    (convos/start-summarization-machine! {} own-puk tuple-base summaries-out @lease)))]

      (try
        (loop [timestamp 0
               pending events]
          (when-let [e (first pending)]
            (if (= e :restart)
              (do
                (restart-subject)
                (recur timestamp (next pending)))
              (do
                (when (contains? e :contact)
                  (<!!? (store-contact {"party" (:contact e) "payload" (:nick e) "timestamp" timestamp})))
                (when-let [label (:recv e)]
                  (let [received-msg (<!!? (store-message {"author" (:auth e) "audience" own-puk "label" label "timestamp" timestamp}))]
                    (swap! label->msg assoc label received-msg)))
                (when-let [label (:send e)]
                  (<!!? (store-message {"author" own-puk "audience" (:audience e) "label" label "timestamp" timestamp})))
                (when-let [label (:read e)]
                  (store-read (:auth e) (@label->msg label)))
                (recur (inc timestamp) (next pending))))))

        (when-not @subject (restart-subject))

        (fact "Events produce expected summaries"
          summaries-out => (<emits expected-summaries))

        (finally
          (fact "machine terminates when lease channel is closed"
            (close! @lease)
            (<!!? @subject) => nil))))))

(let [unknown (keys/->puk "unknown puk")
      ann     (keys/->puk "ann puk")
      jon     (keys/->puk "jon puk")]
  (tabular "Conversation summarization"

    (fact "Events produce expected summaries"
      (summarize! ?events ?expected-summaries))             ; Tabular symbols like ?this need the fact macro to work. That is why this fact has no "=>" operator.

    ?obs
    ?events
    ?expected-summaries

    "Summaries start empty."
    []
    []

    "Message received without known contact is ignored"
    [{:recv "Hello" :auth unknown}]
    []

    "New contact"
    [{:contact ann :nick "Ann"}]
    [{:name "Ann" :timestamp 0}]

    "New contacts and invites"
    [{:contact ann :nick "Ann"}
     {:contact nil :nick "Bob"}
     {:contact jon :nick "Jon"}]
    [{:name "Jon" :timestamp 2}
     ;{:name "Bob" :timestamp 1}
     {:name "Ann" :timestamp 0}]

    "Message received from Ann is unread"
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}]

    [{:name "Ann" :timestamp 1 :preview "Hello" :unread "*"}]

    "Nick change should not affect unread field."
    [:restart
     {:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}
     :restart
     {:contact ann :nick "Annabelle"}]

    [{:name "Annabelle" :timestamp 2 :preview "Hello" :unread "*"}]

    "Any unread message with question mark produces question mark in unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Where is the party??? :)" :auth ann}
     {:recv "Answer me!!" :auth ann}]

    [{:name "Ann" :timestamp 2 :preview "Answer me!!" :unread "?"}]

    "Sent messages appear in the preview."
    [{:contact ann :nick "Ann"}
     {:send "Hi Ann!" :audience ann}]

    [{:name "Ann" :timestamp 1 :preview "Hi Ann!"}]

    "Last message marked as read clears unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Hello1" :auth ann}
     {:recv "Hello2" :auth ann}
     {:read "Hello2" :auth ann}]

    [{:name "Ann" :timestamp 2 :preview "Hello2" :unread ""}]

    "Old message marked as read does not clear unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Hello1" :auth ann}
     {:recv "Hello2" :auth ann}
     {:read "Hello1" :auth ann}]

    [{:name "Ann" :timestamp 2 :preview "Hello2" :unread "*"}]))
