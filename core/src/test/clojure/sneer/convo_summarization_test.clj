(ns sneer.convo-summarization-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!]]
            [sneer.async :refer [sliding-chan]]
            [sneer.commons :refer [submap?]]
            [sneer.test-util :refer [<!!? >!!? <emits tmp-file]]
            [sneer.tuple.protocols :refer :all]
            [sneer.keys :as keys]
            [sneer.convo-summarization :as subject]))

(defn relevant-keys [summary]
  (select-keys summary [:nick :timestamp :preview :unread]))

(defn feed-tuple! [tuples-in tuple]
  (let [timestamp (tuple "timestamp")
        tuple (assoc tuple
                     "id" timestamp
                     "original_id" timestamp)]
    (assert
     (>!!? tuples-in tuple))
    tuple))

(defn- summarize! [events expected-summaries]
  (let [own-puk (keys/->puk "neide puk")

        tuples-in (chan)
        feed-tuple! #(feed-tuple! tuples-in %)

        proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
        feed-contact! #(feed-tuple! (merge proto-contact %))

        proto-message {"type" "message" "message-type" "chat"}
        feed-message! #(feed-tuple! (merge proto-message %))

        label->msg (atom {})
        feed-read! (fn [contact-puk msg] (feed-tuple! {"author" own-puk "type" "message-read" "audience" contact-puk "payload" (msg "original_id")}))

        summaries-out (subject/sliding-summaries! own-puk tuples-in)]

    (loop [timestamp 0
           pending events]
      (when-let [e (first pending)]
        (when (contains? e :contact)
          (feed-contact! {"party" (:contact e) "payload" (:nick e) "timestamp" timestamp}))
        (when-let [label (:recv e)]
          (let [received-msg (feed-message! {"author" (:auth e) "audience" own-puk "label" label "timestamp" timestamp})]
            (swap! label->msg assoc label received-msg)))
        (when-let [label (:send e)]
          (feed-message! {"author" own-puk "audience" (:audience e) "label" label "timestamp" timestamp}))
        (when-let [label (:read e)]
          (feed-read! (:auth e) (@label->msg label)))
        (recur (inc timestamp) (next pending))))

    (fact "Events produce expected summaries"
      summaries-out => (<emits #(= (mapv relevant-keys %) expected-summaries)))

    (fact "Output is closed when input is closed"
      (close! tuples-in)
      (<!!? summaries-out) => nil)))

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
    [{:nick "Ann" :timestamp 0}]

    "New contacts and invites"
    [{:contact ann :nick "Ann"}
     {:contact nil :nick "Bob"}
     {:contact jon :nick "Jon"}]
    [{:nick "Jon" :timestamp 2}
     {:nick "Bob" :timestamp 1}
     {:nick "Ann" :timestamp 0}]

    "Duplicate nick is ignored"
    [{:contact ann :nick "Ann"}
     {:contact jon :nick "Ann"}
     {:recv "Hello" :auth jon}]
    [{:nick "Ann" :timestamp 0}]

    "Message received from Ann is unread"
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}]

    [{:nick "Ann" :timestamp 1 :preview "Hello" :unread "*"}]

    "Nick change should not affect unread field."
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}
     {:contact ann :nick "Annabelle"}]

    [{:nick "Annabelle" :timestamp 2 :preview "Hello" :unread "*"}]

    "Any unread message with question mark produces question mark in unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Where is the party??? :)" :auth ann}
     {:recv "Answer me!!" :auth ann}]

    [{:nick "Ann" :timestamp 2 :preview "Answer me!!" :unread "?"}]

    "Sent messages appear in the preview."
    [{:contact ann :nick "Ann"}
     {:send "Hi Ann!" :audience ann}]

    [{:nick "Ann" :timestamp 1 :preview "Hi Ann!"}]

    "Last message marked as read clears unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Hello1" :auth ann}
     {:recv "Hello2" :auth ann}
     {:read "Hello2" :auth ann}]

    [{:nick "Ann" :timestamp 2 :preview "Hello2" :unread ""}]

    "Old message marked as read does not clear unread status."
    [{:contact ann :nick "Ann"}
     {:recv "Hello1" :auth ann}
     {:recv "Hello2" :auth ann}
     {:read "Hello1" :auth ann}]

    [{:nick "Ann" :timestamp 2 :preview "Hello2" :unread "*"}]))
