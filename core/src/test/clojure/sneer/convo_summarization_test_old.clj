(ns sneer.convo-summarization-test-old
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!]]
            [sneer.async :refer [sliding-chan]]
            [sneer.commons :refer [submap?]]
            [sneer.test-util :refer [<!!? >!!? <emits closes tmp-file]]
            [sneer.integration-test-util :refer [sneer!]]
            [sneer.tuple.protocols :refer :all]
            [sneer.tuple-base-provider :refer [tuple-base-of]]
            [sneer.keys :as keys])
  (:import (sneer.admin SneerAdmin)
           (sneer.interfaces ConvoSummarization)
           (java.io Closeable)))

(defn- pad [summary]
  (-> (merge {:preview "" :unread ""} summary)
      (select-keys [:nick :timestamp :preview :unread])))

(defn- summarize! [events expected-summaries]
  (let [sneer ^Closeable (sneer!)
        admin (sneer SneerAdmin)
        own-puk (.. admin privateKey publicKey)
        tb (tuple-base-of admin)

        feed-tuple! #(<!!? (store-tuple tb %))

        proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
        feed-contact! #(feed-tuple! (merge proto-contact %))

        proto-message {"type" "message" "message-type" "chat"}
        feed-message! #(feed-tuple! (merge proto-message %))

        label->msg (atom {})
        feed-read! (fn [contact-puk msg]
                     (feed-tuple! {"author" own-puk "type" "message-read" "audience" contact-puk "payload" (msg "id")}))

        summaries-out (.slidingSummaries (sneer ConvoSummarization)) ]

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
      summaries-out => (<emits #(= (mapv pad %) (mapv pad expected-summaries))))

    (fact "Output is closed when input is closed"
      (.close sneer)
      summaries-out => closes)))

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