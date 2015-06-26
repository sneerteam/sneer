(ns sneer.contacts-test
  (:require [midje.sweet :refer :all]
            [sneer.async :refer [sliding-chan]]
            [sneer.contacts :as contacts :refer [handle tap-nicks!]]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer! connect! puk]]
            [sneer.test-util :refer [<emits emits-error ->chan <!!? <next]])
  (:import [sneer.flux Dispatcher Action]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "Contacts Test"
  (with-open [sneer (sneer!)]
    (let [subject (sneer contacts/handle)
          nicks (sliding-chan)]

      (tap-nicks! subject nicks)
      nicks => (<emits empty?)

      (.dispatchMap (sneer Dispatcher) {:type  "new-contact"
                                        "nick" "Neide"})
      nicks => (<emits #(% "Neide")))))
