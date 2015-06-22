(ns sneer.convos-test
  (:require [midje.sweet :refer :all]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer!]]
            [sneer.test-util :refer [emits emits-error ->chan <!!? <next]])
  (:import [sneer.convos Convos]
           [sneer.commons.exceptions FriendlyException]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "Convos"
  (with-open [neide (sneer!)]
    (let [convos ^Convos (neide Convos)]
      (. convos summaries) => (emits #(.isEmpty %))
      (. convos problemWithNewNickname "") => "cannot be empty"
      (. convos problemWithNewNickname "Maico") => nil
      (let [convo-id (<next (. convos startConvo "Maico"))]
        convo-id => #(instance? Long %)
        (-> convos (.getById convo-id) <next .nickname) => "Maico")
      (. convos summaries) => (emits #(-> % first .nickname (= "Maico")))
      (. convos problemWithNewNickname "Maico") => "already used"
      (. convos startConvo "Maico") => (emits-error FriendlyException))))

  ; Subs for conversations.
  ; Reads not being emitted by old logic or not being processed by new summarization.
