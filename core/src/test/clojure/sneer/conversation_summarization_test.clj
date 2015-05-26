(ns sneer.conversation-summarization-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!] :as async]
            [sneer.async :refer [sliding-chan]]
            [sneer.commons :refer [submap?]]
            [sneer.test-util :refer [<!!? <wait-for!]]
            [sneer.tuple.jdbc-database :as database]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
            [sneer.tuple.protocols :refer :all]
            [sneer.keys :as keys]
            [sneer.conversations :as convos])
  (:import [sneer.commons Clock]
           [org.h2.mvstore MVStore]
           (java.io File)))


(defn summarize [events expected-summary]

  (with-open [db (database/create-sqlite-db)
              tuple-base (tuple-base/create db)]

    (let [own-puk (keys/->puk "neide puk")

          proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
          store-contact (fn [contact] (store-tuple tuple-base (merge proto-contact contact)))

          proto-message {"type" "message" "message-type" "chat"}
          store-message (fn [tuple] (store-tuple tuple-base (merge proto-message tuple)))

          label->msg (atom {})
          store-read (fn [contact-puk msg] (store-tuple tuple-base {"author" own-puk "type" "message-read" "audience" contact-puk "payload" (msg "original_id")}))

          store-filename (.getAbsolutePath (File/createTempFile "tmp" ".tmp"))
          store (atom nil)
          subject (atom nil)
          summaries-out (chan (async/sliding-buffer 1) (map #(select-keys (first %) [:name :timestamp :date :preview :unread])))
          lease (chan)
          pretty-date-period (chan)
          restart-subject #(do
                            (swap! store (fn [old-store]
                                           (when old-store (.close old-store))
                                           (MVStore/open store-filename)))
                            (reset! subject
                                       (convos/start-summarization-machine! (.openMap @store "test-state") own-puk tuple-base summaries-out pretty-date-period lease)))]

      (try
        (Clock/startMocking)
        (loop [timestamp 0
               pending events]
          (when-let [e (first pending)]
            (if (= e :restart)
              (do
                (restart-subject)
                (recur timestamp (next pending)))
              (do
                (when-let [party (:contact e)]
                  (<!!? (store-contact {"party" party "payload" (:nick e) "timestamp" timestamp})))
                (when-let [label (:recv e)]
                  (let [received-msg (<!!? (store-message {"author" (:auth e) "audience" own-puk "label" label "timestamp" timestamp}))]
                    (swap! label->msg assoc label received-msg)))
                (when-let [label (:send e)]
                  (<!!? (store-message {"author" own-puk "audience" (:audience e) "label" label "timestamp" timestamp})))
                (when-let [label (:read e)]
                  (store-read (:auth e) (@label->msg label)))
                (when-let [millis (:step-millis e)]
                  (Clock/advance millis))
                (recur (inc timestamp) (next pending))))))

        (when-not @subject (restart-subject))

        (or (<wait-for! summaries-out #(submap? expected-summary %))
            :ok)

        (finally
          (Clock/stopMocking)
          (close! lease)
          (fact "machine terminates when lease channel is closed"
            (<!!? @subject) => nil))))))

(let [unknown (keys/->puk "unknown puk")
      ann (keys/->puk "ann puk")]
  (tabular "Conversation summarization"

    (fact "Events produce expected summaries"
      (let [expected-summary ?expected-summary]
        (summarize ?events expected-summary)) => :ok)

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
     :restart
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

      {:name "Ann" :timestamp 2 :preview "Hello2" :unread "*"}

      "Pretty time: now"
      [{:contact ann :nick "Ann"}]
      {:name "Ann" :date "moments from now"}

      "Pretty time: moments ago"
      [{:contact ann :nick "Ann"}
       {:step-millis 10}]
      {:name "Ann" :date "moments ago"}

      "Pretty time: 5 minutes ago"
      [{:contact ann :nick "Ann"}
       {:step-millis (inc (* 1000 60 5))}]
      {:name "Ann" :date "5 minutes ago"}))

; TODO: Process deltas, not entire history.
