(ns sneer.contacts-test
  (:require [midje.sweet :refer :all]
            [sneer.async :refer [sliding-chan]]
            [sneer.contacts :as contacts :refer [handle problem-with-new-nickname]]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer! restarted!]]
            [sneer.flux :refer [action]]
            [sneer.test-util :refer [emits emits-error ->chan <!!? <next]])
  (:import [sneer.flux Dispatcher]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "Contacts Test"
  (with-open [sneer (sneer!)]
    (let [subject (sneer contacts/handle)]

      (problem-with-new-nickname subject "") => (emits "cannot be empty")
      (problem-with-new-nickname subject "Neide") => (emits nil)

      (.dispatch (sneer Dispatcher) (action "new-contact" "nick" "Neide"))
      (.dispatch (sneer Dispatcher) (action "new-contact" "nick" "Neide")) ; Ignored

      (problem-with-new-nickname subject "Neide") => (emits "already used"))

    (with-open [sneer (restarted! sneer)]
      (let [subject (sneer contacts/handle)]
        (problem-with-new-nickname subject "Neide") => (emits "already used")))))

; TODO: reorder args for state-machine to be the same as reduce.
