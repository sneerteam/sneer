(ns sneer.conversation-summarization-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!]]
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
          summaries-out (sliding-chan 1)
          lease (chan)

          proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
          store-contact (fn [contact] (store-tuple tuple-base (merge proto-contact contact)))

          proto-message {"type" "message" "message-type" "chat"}
          store-message (fn [tuple] (store-tuple tuple-base (merge proto-message tuple)))

          subject (atom nil)
          start-subject (fn [] (swap! subject (fn [old]
                                                (assert (nil? old))
                                                (convos/start-summarization-machine own-puk tuple-base summaries-out lease))))]
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
              (when-let [text (:recv e)]
                (<!!? (store-message {"author" (:auth e) "audience" own-puk "label" text "timestamp" timestamp})))
              (when-let [text (:send e)]
                (<!!? (store-message {"author" own-puk "audience" (:audience e) "label" text "timestamp" timestamp})))
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
      (summarize ?events ?summaries) => :ok)

    ?obs
    ?events
    ?summaries

    "Summaries start empty."
    []
    []

    "Message received without contact is ignored"
    [{:recv "Hello" :auth unknown}]
    []

    "Contact new"
    [{:contact ann :nick "Ann"}]
    [{:name "Ann" :timestamp 0 :preview "" :unread ""}]

    "Message received from Ann is unread"
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}]

    [{:name "Ann" :timestamp 1 :preview "Hello" :unread "*"}]

    "Nick change should not affect unread field."
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}
     :summarize
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

    [{:name "Ann" :timestamp 1 :preview "Hi Ann!" :unread ""}]


    ))

    ; TODO: Messages Read
    ; TODO: Date with pretty time. Ex: "3 minutes ago"
    ; TODO: Process deltas, not entire history.
