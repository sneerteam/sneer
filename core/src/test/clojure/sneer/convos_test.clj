(ns sneer.convos-test
  (:require [midje.sweet :refer :all]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer! connect! puk]]
            [sneer.test-util :refer [emits emits-error ->chan <!!? <next]])
  (:import [sneer.convos Convos]
           [sneer.commons.exceptions FriendlyException]
           [sneer.flux Dispatcher]))

; (do (require 'midje.repl) (midje.repl/autotest))

#_(facts "Convos"
  (with-open [neide (sneer!)]
    (let [n-convos ^Convos (neide Convos)]
      (. n-convos summaries) => (emits #(.isEmpty %))
      (. n-convos problemWithNewNickname "") => (emits "cannot be empty")
      (. n-convos problemWithNewNickname "Carla") => (emits nil)
      (let [convo-id (<next (. n-convos startConvo "Carla"))
            n->c-obs (.getById n-convos convo-id)
            n->c (<next n->c-obs)]
        (. n-convos problemWithNewNickname "Carla") => (emits "already used")
        (. n-convos startConvo "Carla") => (emits-error FriendlyException)

        (. n-convos summaries) => (emits #(-> % first .nickname (= "Carla")))
        (.nickname n->c) => "Carla"
        (.inviteCodePending n->c) => some?

        (with-open [carla (sneer!)]
          (connect! neide carla)
          (.acceptInvite (carla Convos) "Neide" (-> neide puk .toHex) (.inviteCodePending n->c))
          n->c-obs => (emits #(-> % .inviteCodePending nil?)))

        (.dispatch (neide Dispatcher) (.setNickname n->c "Carla Costa"))
;        n->c-obs => (emits #(-> % .nickname (= "Carla Costa")))

        ))))

  ; set-nickname action with duplicate nick emits error warning via "toaster"
  ; Subs for conversations.
  ; Reads not being emitted by old logic or not being processed by new summarization.
