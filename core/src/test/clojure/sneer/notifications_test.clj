(ns sneer.notifications-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple-base-provider :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.admin :refer [new-sneer-admin-over-db]]
            [sneer.keys :refer [->puk]]))

(defn- notification-empty? [n]
  (empty? (.conversations n)))

#_(facts "About Conversations.notifications"
  (with-open [neide-db (create-sqlite-db)
              neide-admin (new-sneer-admin-over-db neide-db)
              maico-db (create-sqlite-db)
              maico-admin (new-sneer-admin-over-db maico-db)]
    (let [neide-puk (.. neide-admin privateKey publicKey)
          neide-sneer (.sneer neide-admin)
          neide-tuple-base (tuple-base-of neide-admin)
          maico-puk (.. maico-admin privateKey publicKey)
          maico-sneer (.sneer maico-admin)
          maico-tuple-base (tuple-base-of maico-admin)
          neide (.produceContact maico-sneer "neide" (.produceParty maico-sneer neide-puk) nil)
          maico (.produceContact neide-sneer "maico" (.produceParty neide-sneer maico-puk) nil)
          notifications (observable->chan (subscribe-on-io (.. neide-sneer conversations notifications)))]
      (fact "Starts empty"
        (<!!? notifications) => notification-empty?))))
