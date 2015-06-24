(ns sneer.convos-test
  (:require [midje.sweet :refer :all]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer! connect! puk]]
            [sneer.test-util :refer [emits emits-error ->chan <!!? <next]])
  (:import [sneer.convos Convos]
           [sneer.commons.exceptions FriendlyException]
           [sneer.flux Dispatcher]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "Convos"
  (with-open [neide (sneer!)]
    (let [convos ^Convos (neide Convos)]
      (. convos summaries) => (emits #(.isEmpty %))
      (. convos problemWithNewNickname "") => "cannot be empty"
      (. convos problemWithNewNickname "Maico") => nil
      (let [convo-id (<next (. convos startConvo "Maico"))
            convo-obs (.getById convos convo-id)
            convo (<next convo-obs)]
        (. convos problemWithNewNickname "Maico") => "already used"
        (. convos startConvo "Maico") => (emits-error FriendlyException)
        (. convos summaries) => (emits #(-> % first .nickname (= "Maico")))

        (.nickname convo) => "Maico"
        (.dispatch (neide Dispatcher) (.setNickname convo "Maico Costa"))
;        convo-obs => (emits #(-> % .nickname (= "Maico Costa")))

        (with-open [maico (sneer!)]
          (connect! neide maico)
          #_(-> (maico Convos) (.startconvo "Neide" (-> neide puk .toHex) (.inviteCodePending convo)))

          )))))

  ; set-nickname action with duplicate nick emits error warning via "toaster"
  ; Subs for conversations.
  ; Reads not being emitted by old logic or not being processed by new summarization.
