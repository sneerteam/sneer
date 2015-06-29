(ns sneer.contacts-test
  (:require [midje.sweet :refer :all]
            [sneer.async :refer [sliding-chan]]
            [sneer.contacts :as contacts :refer [handle problem-with-new-nickname nickname #_set-nickname]]
            [sneer.integration-test-util :refer [sneer! restarted!]]
            [sneer.flux :refer [request]]
            [sneer.test-util :refer [emits emits-error ->chan <!!? <next]])
  (:import [sneer.flux Dispatcher]
           [sneer.commons.exceptions FriendlyException]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- -new-contact [sneer nick]
  (.request (sneer Dispatcher) (request "new-contact" "nick" nick)))

(facts "No Contacts"
  (with-open [sneer (sneer!)]
    (let [subject (sneer contacts/handle)]
      (problem-with-new-nickname subject "")      => (emits "cannot be empty")
      (problem-with-new-nickname subject "Neide") => (emits nil))))

(facts "One Contact"
  (with-open [sneer (sneer!)]
    (let [subject (sneer contacts/handle)
          id (<next (-new-contact sneer "Neide"))]

      (fact "Duplicate contacts are bad"
        (problem-with-new-nickname subject "Neide") => (emits "already used")
        (-new-contact sneer "Neide") => (emits-error FriendlyException))

      (fact "Nickname can be changed"
        (let [nick-obs (nickname subject id)]
          nick-obs => (emits "Neide")
;          (set-nickname subject id "Neide Silva")
;          nick-obs => (emits "Neide Silva")
          )))

    (with-open [sneer (restarted! sneer)]
      (let [subject (sneer contacts/handle)]
        (problem-with-new-nickname subject "Neide") => (emits "already used")))))
