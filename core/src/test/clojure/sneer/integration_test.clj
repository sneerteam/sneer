(ns sneer.integration-test
  (:require [midje.sweet :refer :all]
            [sneer.test-util :refer [->chan emits tmp-folder]])
  (:import [sneer.conversations ConversationList]
           [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn container! []
  (let [delegate (Container. (CoreLoader.))]
    (.inject delegate PersistenceFolder
             (let [folder (tmp-folder)] (reify PersistenceFolder
                                          (get [_] folder))))
    (reify
      IFn
      (invoke [_ component]
        (.produce delegate component)))))

(def neide (container!))

#_(facts "Sneer as a whole"
  (. (neide ConversationList) summaries) => (emits #(.isEmpty %)) )
