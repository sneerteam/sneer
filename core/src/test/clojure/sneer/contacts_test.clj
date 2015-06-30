(ns sneer.contacts-test
  (:require [midje.sweet :refer :all]
            [sneer.async :refer [sliding-chan]]
            [sneer.contacts :as contacts :refer [handle invite-code problem-with-new-nickname]]
            [sneer.integration-test-util :refer [sneer! restarted! connect! puk]]
            [sneer.flux :refer [request action]]
            [sneer.test-util :refer [emits emits-error ->chan <!!? <next]])
  (:import [sneer.flux Dispatcher]
           [sneer.commons.exceptions FriendlyException]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- -new-contact [sneer nick]
  (.request (sneer Dispatcher) (request "new-contact" "nick" nick)))

(defn- -accept-invite [sneer nick puk-hex invite-code-received]
  (.request (sneer Dispatcher) (request "accept-invite" "nick" nick "puk-hex" puk-hex "invite-code-received" invite-code-received)))

(facts "No Contacts"
  (with-open [sneer (sneer!)]
    (let [subject (sneer contacts/handle)]
      (problem-with-new-nickname subject "")      => (emits "cannot be empty")
      (problem-with-new-nickname subject "Neide") => (emits nil))))

(facts "One Contact"
  (with-open [neide (sneer!)]
    (let [subject (neide contacts/handle)
          id (<next (-new-contact neide "Carla"))
          invite-obs (invite-code subject id)
          invite (<next invite-obs)]

      invite => some?

      (fact "Duplicate contacts are bad"
        (problem-with-new-nickname subject "Carla") => (emits "already used")
        (-new-contact neide "Carla") => (emits-error FriendlyException))

      (with-open [carla (sneer!)]
        (connect! neide carla)
        (-accept-invite carla "Neide" (-> neide puk .toHex) invite)
        invite-obs => (emits nil)))

    #_(fact "Nickname can be changed"
      (let [nick-obs (nickname subject id)]
        nick-obs => (emits "Neide")
;        (.dispatch (sneer Dispatcher) (action "set-nickname" "nick" "Neide Silva"))
;        nick-obs => (emits "Neide Silva")
        ))
    ))
