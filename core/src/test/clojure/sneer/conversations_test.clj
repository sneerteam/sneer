(ns sneer.conversations-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :as async :refer [thread]]
            [rx.lang.clojure.core :as rx]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
            [sneer.tuple.protocols :refer :all]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.rx :refer [observe-for-io]]
            [sneer.keys :refer [->puk create-prik]]
            [sneer.impl :refer [new-sneer]])
  (:import [rx Observable]
           [sneer Sneer Party Conversations]
           [sneer.commons.exceptions FriendlyException]
           [sneer.tuples TupleSpace]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- pst [fn]
  (try (fn)
       (catch Exception e (.printStackTrace e))))

(defn- ->chan [^Observable o]
  (->> o observe-for-io observable->chan))

(defn- test-produce-contact [sneer nick party]
  (let [->party #(.produceParty sneer (->puk (str % "Party")))]
    (.produceContact sneer nick (when party (->party party)) nil)))

(defn- test-produce-contacts [restart nick party nick2 party2]
  (with-open [db (jdbc-database/create-sqlite-db)]
    (let [own-prik (create-prik)
          own-puk  (.publicKey own-prik)
          tuple-base (tuple-base/create db)
          tuple-space (space/reify-tuple-space own-puk tuple-base)
          sneer (new-sneer tuple-space own-prik)]
      (test-produce-contact sneer nick party)
      (when restart (.close tuple-base))
      (let [tuple-base (if restart (tuple-base/create db) tuple-base)
            tuple-space (if restart (space/reify-tuple-space own-puk tuple-base) tuple-space)
            sneer (if restart (new-sneer tuple-space own-prik) sneer)
            contact (test-produce-contact sneer nick2 party2)]
        (.close tuple-base)
        contact))))

(def ok   truthy)
(def nope (throws FriendlyException))

(tabular "Transient and persistent produceContact scenarios."
         (tabular

           (fact
             (test-produce-contacts ?restart ?nick ?party ?nick2 ?party2)
             => ?result)

           ?nick ?party ?nick2 ?party2 ?result ?count ?obs
           "Ann" nil    "Ann"  "A"     ok      1      "Invited then added"
           "Ann" "A"    "Ann"  "A"     nope    1      "Duplicate contact"
           "Ann" "A"    "Bob"  "B"     ok      2      "Diferent contacts"
           "Ann" "A"    "Ann"  "B"     nope    1      "Nickname already used"
           "Ann" "A"    "Ann"  nil     nope    1      "Nickname already used 2"
           "Ann" "A"    "Bob"  "A"     nope    1      "Ann already has a nick")

  ?restart
  false
  true)

(facts "About reify-conversations"
  (with-open [db (jdbc-database/create-sqlite-db)
              tuple-base (tuple-base/create db)]

    (facts "#all"
      (let [own-prik       (create-prik)
            own-puk        (.publicKey own-prik)
            ^TupleSpace    tuple-space (space/reify-tuple-space own-puk tuple-base)]

        (let [^Sneer         sneer       (new-sneer tuple-space own-prik)

              ^Conversations subject     (.conversations sneer)

              produce-party  #(.produceParty sneer (->puk %))
              ^Party         neide       (produce-party "neide")
              ^Party         carla       (produce-party "carla")

              all-conversations (->chan (->> (.all subject)
                                             (rx/map (partial mapv #(some-> % .contact .nickname .current)))))]
          (fact "is initially empty"
                (<!!? all-conversations) => [])

          (fact "a new contact implies a new converstation"
                (.produceContact sneer "neide" neide nil)
                (<!!? all-conversations) => ["neide"]

                (.produceContact sneer "carla" carla nil)
                (<!!? all-conversations) => ["carla" "neide"]

                (.produceContact sneer "anna" nil nil)
                (<!!? all-conversations) => ["anna" "carla" "neide"]

                (.problemWithNewNickname sneer "anna" carla) => some?))

        (let [^Sneer         sneer-2   (new-sneer tuple-space own-prik)
              ^Conversations subject-2 (.conversations sneer-2)
              all-conversations-2 (->chan (->> (.all subject-2)
                                               (rx/map (partial mapv #(some-> % .contact .nickname .current)))))]
          (fact "contacts are published to the database"
                (<!!? all-conversations-2) => ["anna" "carla" "neide"]))))))