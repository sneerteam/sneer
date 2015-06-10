(ns sneer.convos-test
  (:require [midje.sweet :refer :all]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer!]]
            [sneer.test-util :refer [emits]])
  (:import [sneer.convos Convos]
           [sneer.commons.exceptions FriendlyException]))

; (do (require 'midje.repl) (midje.repl/autotest))

#_(facts "Convos"
  (with-open [neide (sneer!)]
    (let [convos (neide Convos)]
      (. convos summaries) => (emits #(.isEmpty %))
      (. convos problemWithNewNickname "") => "cannot be empty"
      (. convos problemWithNewNickname "Maico") => nil
      (let [convo-id (. convos startConvo "Maico")]
        (.. (.getById convos convo-id) nickname) => (emits "Maico"))
      (. convos summaries) => (emits #(-> % first (.nickname) (= "Maico")))
      (. convos problemWithNewNickname "Maico") => "already used"
      (. convos startConvo "Maico") => (throws FriendlyException))))

  ; Subs for conversations.
  ; Reads not being emitted by old logic or not being porcessed by new summarization.
