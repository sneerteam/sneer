(ns sneer.integration-test
  (:require [midje.sweet :refer :all]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.test-util :refer [->chan emits tmp-folder]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple.persistent-tuple-base :as tb])
  (:import [sneer.conversations ConversationList]
           [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]
           (sneer.crypto.impl KeysImpl)
           (sneer.admin SneerAdmin)
           (sneer Sneer)))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- create-persistence-folder! []
  (let [folder (tmp-folder)]
    (reify PersistenceFolder
      (get [_] folder))))

(defn create-admin! []
  (let [db (create-sqlite-db)
        tb (tb/create db)]
    (new-sneer-admin (.createPrivateKey (KeysImpl.)) tb)))

(defn- inject-sneer! [container]
  (let [admin (create-admin!)]
    (.inject container PersistenceFolder (create-persistence-folder!))
    (.inject container SneerAdmin admin)
    (.inject container Sneer (.sneer admin))))

(defn- sneer-container! []
  (let [delegate (Container. (CoreLoader.))]
    (inject-sneer! delegate)
    (reify IFn (invoke [_ component]
                 (.produce delegate component)))))

(facts "Sneer as a whole"
  (let [neide (sneer-container!)]
    (. (neide ConversationList) summaries) => (emits #(.isEmpty %)))
  ;(. (neide Contacts) problemWithNewNickname) => nil
  )

