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

(facts "About invites"
       (fact "auto-add back when invite code received"
             (with-open [db-c (jdbc-database/create-sqlite-db)
                         sneer-admin-c (start db-c)
                         db-n (jdbc-database/create-sqlite-db)
                         sneer-admin-n (start db-n)]
               (let [puk-c (-> sneer-admin-c .sneer .self .publicKey .current)
                     sneer-n (.sneer sneer-admin-n)
                     sneer-c (.sneer sneer-admin-c)
                     c->n (.produceContact sneer-c "neide" nil nil)
                        _ (.produceContact sneer-n "carla" (.produceParty sneer-n puk-c) (.inviteCode c->n))
                     c->n-parties (->> c->n .party .observable (rx/filter some?))
                     c->n-puks (->> c->n-parties (rx/map party->puk))]
                 (<!!? (->chan c->n-parties) 1000) => #(not (= % :timeout))
                 (<!!? (->chan c->n-puks   ) 1000) => #(not (= % :timeout))
                 (-> c->n .party .current) => some?))))