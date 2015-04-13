(ns sneer.invite-test
  (:require [midje.sweet :refer :all]
            [sneer.admin :refer [new-sneer-admin-over-db]]
            [sneer.tuple.jdbc-database :as jdbc-database]
            [sneer.tuple-base-provider :refer [tuple-base-of]]
            [sneer.tuple.protocols :refer :all]
            [sneer.keys :refer [->puk]]
            [sneer.test-util :refer [<!!? ->chan]]
            [sneer.party :refer [party->puk]]
            [sneer.restartable :refer [restart]]
            [rx.lang.clojure.core :as rx]))

(def neide (->puk "neide"))

(facts "About invites"
       (fact "auto-add back when invite code received"
         (with-open [db (jdbc-database/create-sqlite-db)
                     sneer-admin (new-sneer-admin-over-db db)]
           (let [contact (-> sneer-admin .sneer (.produceContact "neide" nil nil))
                 tuple-base (tuple-base-of sneer-admin)
                 channel (->> contact .party .observable (rx/filter some?) (rx/map party->puk) ->chan)]
             (store-tuple tuple-base {"type"        "push"
                                      "author"      neide
                                      "audience"    (.. sneer-admin privateKey publicKey)
                                      "invite-code" (.inviteCode contact)})
             (<!!? channel) => neide)
           (with-open [sneer-admin (restart sneer-admin)]
             (-> sneer-admin .sneer .contacts .toBlocking .first first .party .current) => some?)))

       (fact "when adding a contact with an invite code, a push tuple is saved"
         (with-open [db (jdbc-database/create-sqlite-db)
                     sneer-admin (new-sneer-admin-over-db db)]
           (let [party (-> sneer-admin .sneer (.produceParty neide))
                 _ (-> sneer-admin .sneer (.produceContact "neide" party "test123"))
                 filter (-> sneer-admin .sneer .tupleSpace .filter (.type "push") .tuples)]
             (-> filter .toBlocking .first) => some?))))