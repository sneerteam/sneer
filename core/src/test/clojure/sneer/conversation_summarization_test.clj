(ns sneer.conversation-summarization-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!]]
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
          summaries-out (chan)
          lease (chan)

          proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
          store-contact (fn [contact] (store-tuple tuple-base (merge proto-contact contact)))

          proto-message {"type" "message" "audience" own-puk "message-type" "chat"}
          store-message (fn [tuple] (store-tuple tuple-base (merge proto-message tuple)))

          subject (atom nil)
          summarize (fn [] (swap! subject (convos/start-summarization-machine own-puk tuple-base summaries-out lease)))

          _ (loop [timestamp 0
                   pending-events events]
              (let [e (first pending-events)]
                (if (= e :summarize)
                  (summarize)
                  (do
                    (when-let [party (:contact e)]
                      (<!!? (store-contact {"party" party "payload" (:nick e) "timestamp" timestamp})))
                    (when-let [text (:recv e)]
                      (<!!? (store-message {"author" (:auth e) "label" text "timestamp" timestamp}))))))
              (when-let [pending-events' (next pending-events)]
                (recur (inc timestamp) pending-events')))]

      (when-not @subject (summarize))

      (try

        (<wait-for! summaries-out summaries)

        (finally
          (close! lease)
          (fact "machine terminates when lease channel is closed"
            (<!!? @subject) => nil))))))


(let [unknown (keys/->puk "unknown puk")
      ann (keys/->puk "ann puk")]
  (tabular "Conversation summarization"

    (fact "Events produce expected summaries"
      (summarize ?events ?summaries) => #(not (= % :timeout)))

    ; Contact new
    ; Contact change nick
    ; Message received
    ; Message sent
    ; Messages Read

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

    "Nick change should not affect other summary fields."
    [{:contact ann :nick "Ann"}
     {:recv "Hello" :auth ann}
     :summarize
     {:contact ann :nick "Annabelle"}]

    [{:name "Annabelle" :timestamp 1 :preview "Hello" :unread "*"}]))
