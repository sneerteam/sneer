(ns sneer.conversation-summarization-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!] :as async]
            [sneer.async :refer [sliding-chan]]
            [sneer.test-util :refer [<!!? <wait-for!]]
            [sneer.tuple.jdbc-database :as database]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
            [sneer.tuple.protocols :refer :all]
            [sneer.keys :as keys]
            [sneer.conversations :as convos]))


(defn summarize [events summaries]

  (with-open [db (database/create-sqlite-db)
              tuple-base (tuple-base/create db)]

    (let [own-puk (keys/->puk "neide puk")

          proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
          store-contact (fn [contact] (store-tuple tuple-base (merge proto-contact contact)))

          proto-message {"type" "message" "message-type" "chat"}
          store-message (fn [tuple] (store-tuple tuple-base (merge proto-message tuple)))

          label->id (atom {})
          store-read (fn [contact-puk msg-id] (store-tuple tuple-base {"author" own-puk "type" "message-read" "audience" contact-puk "payload" msg-id}))

          subject (atom nil)
          summaries-out (chan (async/sliding-buffer 1) (map #(select-keys (first %) [:name :timestamp :preview :unread])))
          lease (chan)
          start-subject (fn [] (swap! subject (fn [old]
                                                (assert (nil? old))
                                                (convos/start-summarization-machine! own-puk tuple-base summaries-out lease))))]
      (loop [timestamp 0
             pending events]
        (when-let [e (first pending)]
          (if (= e :summarize)
            (do
              (start-subject)
              (recur timestamp (next pending)))
            (do
              (when-let [party (:contact e)]
                (<!!? (store-contact {"party" party "payload" (:nick e) "timestamp" timestamp})))
              (when-let [label (:recv e)]
                (let [received-msg (<!!? (store-message {"author" (:auth e) "audience" own-puk "label" label "timestamp" timestamp}))]
                  (swap! label->id assoc label (received-msg "id"))))
              (when-let [label (:send e)]
                (<!!? (store-message {"author" own-puk "audience" (:audience e) "label" label "timestamp" timestamp})))
              (when-let [label (:read e)]
                (store-read (:auth e) (@label->id label)))
              (recur (inc timestamp) (next pending))))))

      (when-not @subject (start-subject))

      (try
        (or (<wait-for! summaries-out summaries) :ok)

        (finally
          (close! lease)
          (fact "machine terminates when lease channel is closed"
            (<!!? @subject) => nil))))))

(let [unknown (keys/->puk "unknown puk")
      ann (keys/->puk "ann puk")]
  (tabular "Conversation summarization"

    (fact "Events produce expected summaries"
      (summarize ?events ?expected-summary) => :ok)

    ?obs
    ?events
    ?expected-summary

    "Summaries start empty."
    []
    {}

    "Message received without contact is ignored"
    [{:recv "Hello" :auth unknown}]
    {}

    "New contact"
    [{:contact ann :nick "Ann"}]
    {:name "Ann" :timestamp 0}

    "Message received from Ann is unread"
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}]

    {:name "Ann" :timestamp 1 :preview "Hello" :unread "*"}

    "Nick change should not affect unread field."
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}
     :summarize
     {:contact ann :nick "Annabelle"}]

    {:name "Annabelle" :timestamp 2 :preview "Hello" :unread "*"}

    "Any unread message with question mark produces question mark in unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Where is the party??? :)" :auth ann}
     {:recv "Answer me!!" :auth ann}]

    {:name "Ann" :timestamp 2 :preview "Answer me!!" :unread "?"}

    "Sent messages appear in the preview."
    [{:contact ann :nick "Ann"}
     {:send "Hi Ann!" :audience ann}]

    {:name "Ann" :timestamp 1 :preview "Hi Ann!"}

    "Last message marked as read clears unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Hello1" :auth ann}
     {:recv "Hello2" :auth ann}
     {:read "Hello2" :auth ann}]

    {:name "Ann" :timestamp 2 :preview "Hello2" :unread ""}

      "Old message marked as read does not clear unread status."
      [{:contact ann :nick "Ann"}
       {:recv "Hello1" :auth ann}
       {:recv "Hello2" :auth ann}
       {:read "Hello1" :auth ann}]

      {:name "Ann" :timestamp 2 :preview "Hello2" :unread "*"}))

; TODO: Process deltas, not entire history.
; TODO: Identify read messages by author and original id, so senders can ask for them and display "message was read" signs.
; TODO: Date with pretty time. Ex: "3 minutes ago"
