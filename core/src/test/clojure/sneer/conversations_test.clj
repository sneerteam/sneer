(ns sneer.conversations-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :as async :refer [thread]]
            [rx.lang.clojure.core :as rx]
            [sneer.tuple.space :as space]
            [sneer.tuple.persistent-tuple-base :as tuple-base]
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

(defn- start-sneer! [db tuple-base-atom]
  (reset! tuple-base-atom (tuple-base/create db))
  (let [own-prik (create-prik)
        own-puk  (.publicKey own-prik)
        space    (space/reify-tuple-space own-puk @tuple-base-atom)]
    (new-sneer space own-prik)))

(defn- start-sneer2! [db tuple-base-atom restart sneer1]
  (if restart
    (do
      (.close @tuple-base-atom)
      (start-sneer! db tuple-base-atom))
    sneer1))

(defn- test-produce-contact [sneer nick party]
  (let [->party #(.produceParty sneer (->puk (str % "Party")))]
    (.produceContact sneer nick (when party (->party party)) nil)))

(defn- test-produce-contacts [restart nick party nick2 party2]
  (with-open [db (jdbc-database/create-sqlite-db)]
    (let [tuple-base  (atom nil)
          sneer1      (start-sneer!  db tuple-base)
          sneer2     #(start-sneer2! db tuple-base restart sneer1)]
      (test-produce-contact  sneer1  nick  party )
      (test-produce-contact (sneer2) nick2 party2))))

(def ok   truthy)
(def nope (throws FriendlyException))

(tabular "Transient and persistent produceContact scenarios."
         (tabular

           (fact
             (test-produce-contacts ?restart ?nick ?party ?nick2 ?party2)
             => ?result)

           ?nick ?party ?nick2 ?party2 ?result ?obs
           "Ann" nil "Ann" "A" ok "Invited then added"
           ;"Ann" "A"    "Ann"  "A"     ok      "Same contact"
           "Ann" "A"    "Bob"  "B"     ok      "Diferent contacts"
           ;"Ann" "A"    "Ann"  "B"     nope    "Nickname already used"
           ;"Ann" "A"    "Ann"  nil     nope    "Nickname already used 2"
           ;"Ann" "A"    "Bob"  "A"     nope    "Contact already has a nick"
           )

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
                                             (rx/map (partial mapv #(-> % .nickname .current)))))]
          (fact "is initially empty"
                (<!!? all-conversations) => [])

          (fact "a new contact implies a new converstation"
                (. sneer produceContact "neide" neide nil)
                (<!!? all-conversations) => ["neide"]

                (. sneer produceContact "carla" carla nil)
                (<!!? all-conversations) => ["carla" "neide"]

                (. sneer produceContact "anna" nil nil)
                (<!!? all-conversations) => ["anna" "carla" "neide"]))

        (let [^Sneer         sneer-2   (new-sneer tuple-space own-prik)
              ^Conversations subject-2 (.conversations sneer-2)
              all-conversations-2 (->chan (->> (.all subject-2)
                                               (rx/map (partial mapv #(-> % .nickname .current)))))]
          (fact "contacts are published to the database"
                (<!!? all-conversations-2) => ["anna" "carla" "neide"]))))))
