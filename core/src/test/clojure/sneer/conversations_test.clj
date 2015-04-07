(ns sneer.conversations-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :as async :refer [thread]]
            [rx.lang.clojure.core :as rx]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as base]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.rx :refer [observe-for-io]]
            [sneer.keys :refer [->puk create-prik]]
            [sneer.impl :refer [new-sneer]])
  (:import [rx Observable]
           [sneer Sneer Party Conversations]
           [sneer.commons.exceptions FriendlyException]
           [sneer.tuples TupleSpace]))

(defn- ->chan [^Observable o]
  (->> o observe-for-io observable->chan))

(facts "About reify-conversations"
  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (base/create db)]

    (facts "#all"
      (let [own-prik       (create-prik)
            own-puk        (.publicKey own-prik)
            ^TupleSpace    tuple-space (space/reify-tuple-space own-puk tuple-base)]

        (let [^Sneer         sneer       (new-sneer tuple-space own-prik)

              ^Conversations subject     (.conversations sneer)

              produce-party  #(.produceParty sneer (->puk %))
              ^Party         neide       (produce-party "neide")
              ^Party         carla       (produce-party "carla")
              ^Party         anna       (produce-party "anna")

              all-conversations (->chan (->> (.all subject)
                                             (rx/map (partial mapv #(-> % .nickname .current)))))]
          (fact "is initially empty"
                (<!!? all-conversations) => [])

          (fact "a new contact implies a new converstation"
                (. sneer addContact "neide" neide nil)
                (<!!? all-conversations) => ["neide"]

                (. sneer addContact "carla" carla nil)
                (<!!? all-conversations) => ["carla" "neide"]

                (. sneer addContact "anna" nil "1234")
                (<!!? all-conversations) => ["anna" "carla" "neide"])

          (fact "same nickname for same party is ok"
                (.problemWithNewNickname sneer "neide" neide) => nil)

          (fact "same nickname without a party is ok"
                (.problemWithNewNickname sneer "neide" nil) => nil)

          (fact "same nickname for different party is not ok"
                (. sneer addContact "neide" carla nil) => (throws FriendlyException))

          (fact "same nickname for non-existent party is not ok"
                (. sneer addContact "neide" anna nil) => (throws FriendlyException))

          #_(fact "new nickname for non-existent party is ok"
                (. sneer addContact "anna" anna nil) => some?))

        (let [^Sneer         sneer-2   (new-sneer tuple-space own-prik)
              ^Conversations subject-2 (.conversations sneer-2)
              all-conversations-2 (->chan (->> (.all subject-2)
                                               (rx/map (partial mapv #(-> % .nickname .current)))))]
          (fact "contacts are published to the database"
                (<!!? all-conversations-2) => ["anna" "carla" "neide"]))))))
