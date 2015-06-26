(ns sneer.contacts-test
  (:require [midje.sweet :refer :all]
            [sneer.async :refer [sliding-chan]]
            [sneer.contacts :as contacts :refer [handle tap-nicks!]]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer! restarted!]]
            [sneer.test-util :refer [<emits emits-error ->chan <!!? <next]])
  (:import [sneer.flux Dispatcher Action]))

; (do (require 'midje.repl) (midje.repl/autotest))

#_(facts "Contacts Test"
  (with-open [sneer (sneer!)]
    (let [subject (sneer contacts/handle)
          nicks (sliding-chan)]

      (tap-nicks! subject nicks)
      nicks => (<emits #(nil? ((% :nick->id) "Neide")))

      (.dispatchMap (sneer Dispatcher) {:type  "new-contact"
                                        "nick" "Neide"})
      nicks => (<emits #((% :nick->id) "Neide")))

    (with-open [sneer (restarted! sneer)]
      (let [subject (sneer contacts/handle)
            nicks (sliding-chan)]
        (tap-nicks! subject nicks)
        nicks => (<emits #((% :nick->id) "Neide"))))))
