(ns sneer.integration-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [close!]]
            [sneer.test-util :refer [emits]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]])
  (:import [sneer.convos Convos]
           [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]
           [java.io Closeable]
           [sneer.async LeaseHolder]
           [sneer.tuple.protocols Database]
           [sneer.commons.exceptions FriendlyException]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- sneer! []
  (let [delegate (Container. (CoreLoader.))
        transient nil]
    (.inject delegate PersistenceFolder (reify PersistenceFolder (get [_] transient)))
    (.inject delegate Database (create-sqlite-db))

    (reify IFn (invoke [_ component]
                 (.produce delegate component))
      Closeable (close [_]
                  (close! (.getLeaseChannel (.produce delegate LeaseHolder)))))))


#_(facts "Sneer as a whole"
  (with-open [neide (sneer!)]
    (let [convos (neide Convos)]
      (. convos summaries) => (emits #(.isEmpty %))
      (. convos problemWithNewNickname "Maico") => nil
      (. convos startConvo "Maico")
      (. convos problemWithNewNickname "Maico") => "is already a contact"
      (. convos startConvo "Maico") => (throws FriendlyException)
      (. convos summaries)) => (emits #(-> % first (.party) (= "Maico"))))


  ; Invites must appear in summarization.
  ; Subs for conversations.
  ; Reads not being emitted by old logic or not being porcessed by new summarization.


  )

