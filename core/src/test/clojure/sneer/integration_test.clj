(ns sneer.integration-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [chan]]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.test-util :refer [->chan emits tmp-folder <emits]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple.persistent-tuple-base :as tb])
  (:import [sneer.conversations ConversationList]
           [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]
           [sneer.crypto.impl KeysImpl]
           [sneer.admin SneerAdmin]
           [sneer Sneer]
           [java.io Closeable]
           [sneer.tuple.protocols TupleBase Database]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- create-persistence-folder! []
  (let [folder (tmp-folder)]
    (reify PersistenceFolder
      (get [_] folder))))

(defn- inject-sneer! [container]
  (let [db (create-sqlite-db)
        tb (tb/create db)
        admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) tb)]
    (.inject container PersistenceFolder (create-persistence-folder!))
    (.inject container Database db)
    (.inject container TupleBase tb)
    (.inject container SneerAdmin admin)
    (.inject container Sneer (.sneer admin))))

(defn- sneer-container! []
  (let [delegate (Container. (CoreLoader.))]
    (inject-sneer! delegate)
    (reify IFn       (invoke [_ component]
                       (.produce delegate component))
           Closeable (close [_]
                       (.close (.produce delegate TupleBase))
                       (.close (.produce delegate Database))))))

(facts "Sneer as a whole"
  (with-open [neide (sneer-container!)]
    (dotimes [n 1]
      (. (neide ConversationList) summaries) => (emits #(.isEmpty %))))
  ;(. (neide Contacts) problemWithNewNickname) => nil
  )


