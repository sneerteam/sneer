(ns sneer.conversation-summarization-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan close!]]
            [sneer.test-util :refer [<!!?]]
            [sneer.tuple.jdbc-database :as database]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
            [sneer.tuple.protocols :refer :all]
            [sneer.keys :as keys]
            [sneer.conversations :as convos]))

(facts "about the summarization machine"

  (with-open [db (database/create-sqlite-db)
              tuple-base (tuple-base/create db)]

    (let [own-puk (keys/->puk "neide")
          maico (keys/->puk "maico")
          alice (keys/->puk "alice")
          jonas (keys/->puk "jonas")
          summaries-out (chan)
          lease (chan)

          proto-contact {"type" "contact" "audience" own-puk "author" own-puk}
          store-contact (fn [tuple] (store-tuple tuple-base (merge proto-contact tuple)))

          proto-message {"type" "message" "audience" own-puk "message-type" "chat"}
          store-message (fn [tuple] (store-tuple tuple-base (merge proto-message tuple)))

          pending-stores [(store-contact {"timestamp" 42 "payload" "maico" "party" maico})
                          (store-contact {"timestamp" 51 "payload" "alice" "party" alice})]

                         (doseq [ps pending-stores] (<!!? ps)) ; wait for all stores to happen

          subject (convos/start-summarization-machine own-puk tuple-base summaries-out lease)]

      (try

        (fact "emit summary at start"
          (<!!? summaries-out) => [{:name "alice" :summary "" :unread 0 :timestamp 51}
                                   {:name "maico" :summary "" :unread 0 :timestamp 42}])

        (fact "new contact causes summary update"
          (store-contact {"timestamp" 63 "payload" "jonas" "party" jonas})
          (<!!? summaries-out) => #(->> % (mapv :timestamp) (= [63 51 42])))

        (fact "message from contact causes summary update"
          (store-message {"author" maico "label" "let's go" "timestamp" 70})
          (<!!? summaries-out) =>  [{:name "maico" :unread 1 :timestamp 70 :summary "let's go"}
                                    {:name "jonas" :unread 0 :timestamp 63 :summary ""}
                                    {:name "alice" :unread 0 :timestamp 51 :summary ""}]

          (store-message {"author" alice "label" "ho" "timestamp" 80})
          (<!!? summaries-out) => #(->> % (mapv (juxt :name :summary)) (= [["alice" "ho"]
                                                                           ["maico" "let's go"]
                                                                           ["jonas" ""]])))

        (fact "own messages causes summary update"
          (store-message {"author" own-puk "audience" jonas "label" "hey" "timestamp" 90})
          (<!!? summaries-out) => #(->> % (mapv (juxt :name :summary)) (= [["jonas" "hey"]
                                                                           ["alice" "ho"]
                                                                           ["maico" "let's go"]])))

        (finally
          (close! lease)))

      (fact "machine terminates when lease channel is closed"
        (<!!? subject) => nil))))
