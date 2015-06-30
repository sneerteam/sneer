(ns sneer.convos-test
  (:require [midje.sweet :refer :all]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer!]]
            [sneer.test-util :refer [emits emits-error ->chan <!!?]])
  (:import [sneer.convos Convos]
           [sneer.commons.exceptions FriendlyException]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "Convos"
  (with-open [neide (sneer!)]
    (let [convos ^Convos (neide Convos)]
      (. convos summaries) => (emits #(.isEmpty %))
      (. convos problemWithNewNickname "") => "cannot be empty"
      (. convos problemWithNewNickname "Maico") => nil
      (let [convo-id (<!!? (->chan (. convos startConvo "Maico")))]
        convo-id => #(instance? Long %)
        (.getById convos convo-id) => (emits #(-> % .nickname (= "Maico"))))
      (. convos summaries) => (emits #(-> % first .nickname (= "Maico")))
      (. convos problemWithNewNickname "Maico") => "already used"
      (. convos startConvo "Maico") => (emits-error FriendlyException))))

  ; Subs for conversations.
  ; Reads not being emitted by old logic or not being porcessed by new summarization.
