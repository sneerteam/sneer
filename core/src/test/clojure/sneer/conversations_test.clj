(ns sneer.conversations-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [thread]]
            [rx.lang.clojure.core :as rx]
            [sneer.admin :refer [new-sneer-admin-over-db]]
            [sneer.commons :refer [dispose]]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
            [sneer.tuple.protocols :refer :all]
            [sneer.test-util :refer [<!!? observable->chan ->chan]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.keys :refer [->puk create-prik]]
            [sneer.impl :refer [new-sneer]]
            [sneer.restartable :refer [restart]])
  (:import [sneer Sneer Party Conversations]
           [sneer.commons.exceptions FriendlyException]
           [sneer.tuples TupleSpace]))

(defn- test-produce-contact [sneer nick party]
  (let [->party #(.produceParty sneer (->puk (str % "Party")))]
    (.produceContact sneer nick (when party (->party party)) nil)))

(defn- test-produce-contacts [db restart? nick party nick2 party2]
  (with-open [sneer-admin (new-sneer-admin-over-db db)]
    (test-produce-contact (.sneer sneer-admin) nick party)
    (with-open [sneer-admin (if restart? (restart sneer-admin) sneer-admin)]
      (test-produce-contact (.sneer sneer-admin) nick2 party2)))
  true)

(def ok   truthy)
(def nope (throws FriendlyException))

(tabular "Transient and persistent produceContact scenarios"
         (tabular

           (with-open [db (jdbc-database/create-sqlite-db)]
             (fact "produce contacts"
               (test-produce-contacts db ?restart ?nick ?party ?nick2 ?party2)
               => ?result)
             (fact "count contacts"
                   (with-open [sneer-admin (new-sneer-admin-over-db db)]
                     (->> sneer-admin .sneer .contacts .toBlocking .first .size))
               => ?count))

           ?nick ?party ?nick2 ?party2 ?result ?count ?obs
           "Ann" nil    "Ann"  "A"     ok      1      "Invited then added"
           "Ann" "A"    "Ann"  "A"     ok      1      "Same nickname for same party"
           "Ann" "A"    "Bob"  "B"     ok      2      "Different contacts"
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
              ^Party         anna        (produce-party "anna")

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

                (.withContact subject (.findByNick sneer "anna")) => some?)



          (fact "the nickname 'anna' is already used"
                (.problemWithNewNickname sneer "anna" carla) => some?)

          (fact "existing contact cannot be added to new invite"
                (.produceContact sneer "anna" carla nil) => nope)

          (fact "new party can be added to new invite"
                (.produceContact sneer "anna" anna nil) => ok
                (-> (.findByNick sneer "anna") .party .current) => anna)

          (fact "no problem setting the same nickname for the same party"
                (.problemWithNewNickname sneer "anna2" anna) => nil)

          (fact "finding by nickname"
                (.findByNick sneer "anna") => some?
                (.findByNick sneer "wxyz") => nil)

          (fact "conversations without a party cannot send messages"
                (let [contact (.produceContact sneer "bob" nil nil)
                      conversation (.withContact subject contact)
                      channel (->chan (.canSendMessages conversation))]
                  (<!!? channel) => false)))

        (let [^Sneer         sneer-2   (new-sneer tuple-space own-prik)
              ^Conversations subject-2 (.conversations sneer-2)
              all-conversations-2 (->chan (->> (.all subject-2)
                                               (rx/map (partial mapv #(some-> % .contact .nickname .current)))))]
          (fact "contacts are published to the database"
                (<!!? all-conversations-2) => ["anna" "bob" "carla" "neide"]))))))