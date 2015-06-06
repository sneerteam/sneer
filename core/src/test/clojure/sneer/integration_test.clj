(ns sneer.integration-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [close! chan mult >!! <!!]]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.async :refer [getLeaseChannel sliding-chan]]
            [sneer.test-util :refer [->chan emits tmp-folder <emits <!!?]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple.persistent-tuple-base :as tb])
  (:import [sneer.conversations ConversationList]
           [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]
           [java.io Closeable]
           [sneer.async LeaseHolder]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- sneer-container! []
  (let [delegate (Container. (CoreLoader.))
        transient nil]
    (.inject delegate PersistenceFolder (reify PersistenceFolder (get [_] transient)))
    (reify IFn (invoke [_ component]
                 (.produce delegate component))
      Closeable (close [_]
                  (close! (.getLeaseChannel (.produce delegate LeaseHolder)))))))

(facts "Sneer as a whole"
  (with-open [neide (sneer-container!)]
    (. (neide ConversationList) summaries) => (emits #(.isEmpty %)))
  ;(. (neide Contacts) problemWithNewNickname) => nil
  )

