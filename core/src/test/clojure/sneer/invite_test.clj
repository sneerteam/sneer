(ns sneer.invite-test
  (:require [midje.sweet :refer :all]
            [sneer.admin :refer [new-sneer-admin-over-db]]
            [sneer.main :refer [start]]
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
             (with-open [db-1 (jdbc-database/create-sqlite-db)
                         sneer-admin-1 (start db-1)
                         db-2 (jdbc-database/create-sqlite-db)
                         sneer-admin-2 (start db-2)]
               (let [contact-1 (-> sneer-admin-1 .sneer (.produceContact "neide" nil nil))
                     _ (-> sneer-admin-2 .sneer (.produceContact "carla" (-> sneer-admin-1 .sneer .self) (.inviteCode contact-1)))
                     filter-1 (-> sneer-admin-1 .sneer .tupleSpace .filter (.type "push") .tuples)
                     filter-2 (-> sneer-admin-2 .sneer .tupleSpace .filter (.type "push") .tuples)
                     channel (->> contact-1 .party .observable (rx/filter some?) (rx/map party->puk) ->chan)]
                 ; user 2 successfully saved a push tuple
                 (-> filter-2 .toBlocking .first) => some?
                 ; user 1 successfully received a push tuple
                 (-> filter-1 .toBlocking .first) => some?
                 ; user 1 set the party for the contact
                 (<!!? channel)
                 (-> contact-1 .party .current) => some?))))