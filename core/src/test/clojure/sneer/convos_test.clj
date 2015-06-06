(ns sneer.convos-test
  (:require [midje.sweet :refer :all]
            [sneer.integration-test-util :refer [sneer!]]
            [sneer.test-util :refer [emits]])
  (:import [sneer.convos Convos]
           [sneer.commons.exceptions FriendlyException]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "Convos"
  (with-open [neide (sneer!)]
    (let [convos (neide Convos)]
      (. convos summaries) => (emits #(.isEmpty %))
      (. convos problemWithNewNickname "Maico") => nil
      (. convos startConvo "Maico")
;      (. convos problemWithNewNickname "Maico") => "is already a contact"
;      (. convos startConvo "Maico") => (throws FriendlyException)
;      (. convos summaries) => (emits #(-> % first (.nickname) (= "Maico")))
      ))


  ; Invites must appear in summarization.
  ; Subs for conversations.
  ; Reads not being emitted by old logic or not being porcessed by new summarization.


  )

