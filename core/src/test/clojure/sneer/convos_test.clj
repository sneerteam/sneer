(ns sneer.convos-test
  (:require [midje.sweet :refer :all]
            [sneer.convos :refer :all] ; Force compilation
            [sneer.integration-test-util :refer [sneer! connect! puk]]
            [sneer.rx-test-util :refer [emits emits-error ->chan <next]]
            [sneer.test-util :refer [<!!?]]
            [sneer.flux :refer [dispatch]])
  (:import [sneer.convos Convo Convos ChatMessage Summary]
           [sneer.commons.exceptions FriendlyException]
           [sneer.flux Dispatcher]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn text [^ChatMessage m]
  (.text m))

(defn own? [^ChatMessage m]
  (.isOwn m))

(defn unread [^Summary summary]
  (.unread summary))

(defn last-message-received [^Convo convo]
  (->> convo .messages (remove own?) last))

(defn emits-messages [& ms]
  (emits #(->> % .messages (mapv text) (= ms))))

(facts "Convos"
  (with-open [neide (sneer!)]
    (let [n-convos ^Convos (neide Convos)]
      (. n-convos summaries) => (emits #(.isEmpty %))

      (. n-convos problemWithNewNickname "")      => (emits "cannot be empty")
      (. n-convos problemWithNewNickname "Ca")    => (emits nil)
      (. n-convos problemWithNewNickname "Car")   => (emits nil)
      (. n-convos problemWithNewNickname "Carla") => (emits nil)

      (let [convo-id (<next (. n-convos startConvo "Carla"))
            n->c-obs (.getById n-convos convo-id)
            n->c (<next n->c-obs)]
        (. n-convos problemWithNewNickname "Carla") => (emits "already used")
        (. n-convos startConvo "Carla") => (emits-error FriendlyException)

        (. n-convos summaries) => (emits #(-> % first .nickname (= "Carla")))
        (.nickname n->c) => "Carla"
        (.inviteCodePending n->c) => some?

        (. n-convos startConvo "Maico" )
        (. n-convos summaries) => (emits #(->> % (mapv (fn [summary] (.nickname summary))) (= ["Maico" "Carla"])))

        (with-open [carla (sneer!)]
          (connect! neide carla)

          (let [c-convos (carla Convos)
                inviteCode (.inviteCodePending n->c)
                _ (.acceptInvite c-convos
                                 "Neide"
                                 inviteCode)
                accepted-convo-id (<next (.findConvo c-convos inviteCode))
                c->n-obs (.getById c-convos accepted-convo-id)
                c->n (<next c->n-obs)]

            n->c-obs => (emits #(-> % .inviteCodePending nil?))

            ;New way with less Java dependencies:
            (dispatch (neide Dispatcher) "send-message"
                      ["contact-id" (.id n->c)
                       "text", "hi"])

            n->c-obs => (emits-messages "hi")
            c->n-obs => (emits-messages "hi")

            (.dispatch (carla Dispatcher) (.sendMessage c->n "hello"))
            n->c-obs => (emits-messages "hi" "hello")
            c->n-obs => (emits-messages "hi" "hello")

            (let [convo (<next c->n-obs)]
              (. c-convos summaries) => (emits #(->> % (mapv unread) (= ["*"])))
              (->> (.setRead convo (last-message-received convo)) (.dispatch (carla Dispatcher)))
              (. c-convos summaries) => (emits #(->> % (mapv unread) (= [""]))))))

        (.request (neide Dispatcher) (.setNickname n->c "Carla Costa"))
        n->c-obs => (emits #(-> % .nickname (= "Carla Costa")))))))
