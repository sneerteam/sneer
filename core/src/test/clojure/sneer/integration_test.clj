(ns sneer.integration-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [close!]]
            [sneer.test-util :refer [emits]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]])
  (:import [sneer.conversations ConversationList]
           [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]
           [java.io Closeable]
           [sneer.async LeaseHolder]
           [sneer.tuple.protocols Database]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- sneer-container! []
  (let [delegate (Container. (CoreLoader.))
        transient nil]
    (.inject delegate PersistenceFolder (reify PersistenceFolder (get [_] transient)))
    (.inject delegate Database (create-sqlite-db))

    (reify IFn (invoke [_ component]
                 (.produce delegate component))
      Closeable (close [_]
                  (close! (.getLeaseChannel (.produce delegate LeaseHolder)))))))

(facts "Sneer as a whole"
  (with-open [neide (sneer-container!)]
    (. (neide ConversationList) summaries) => (emits #(.isEmpty %)))
  ;(. (neide Contacts) problemWithNewNickname) => nil
  )

