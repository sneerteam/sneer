(ns sneer.convo
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :as async :refer [<! >! chan]]
            [sneer.async :refer [state-machine go-trace go-loop-trace sliding-chan close-with!]]
            [sneer.contacts]
            [sneer.rx :refer [pipe-to-subscriber! close-on-unsubscribe! shared-latest]]
            [sneer.tuple.protocols :refer :all]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.persistent-tuple-base :refer [after-id]])
  (:import
    [sneer.convos Convo]
    [rx Subscriber]
    [sneer.admin SneerAdmin]))

(defn- tuple-base [container]
  (tuple-base-of (.produce container SneerAdmin)))

(defn- start!
  "`id' is the id of the first contact tuple for this party"
  [container id state-out lease]
  (let [xconvo (comp (map #(sneer.contacts/by-id id %))
                     (filter some?))
        contacts (sneer.contacts/from container)
        contacts-in (sneer.contacts/tap contacts)]
    (close-with! lease contacts-in)
    (async/pipeline 1 state-out xconvo contacts-in)))

; public Convo(long contactId, String nickname, String inviteCodePending, List<ChatMessage> messages, List<SessionSummary> sessionSummaries)
; public SessionSummary(long id, String type, String title, String date, String unread)
; interface Chat { List<Message> messages(); }
; public Message(long id, String text, boolean isOwn, String date)
(defn- to-foreign [{:keys [id nick invite-code]}]
  (Convo. id nick invite-code nil nil))

(defn convo-by-id [container id]
  (rx/observable*
   (fn [^Subscriber subscriber]
     (let [state-out (sliding-chan 1 (map to-foreign))
           lease (chan)]
       (close-on-unsubscribe! subscriber state-out lease)
       (pipe-to-subscriber! state-out subscriber "convo")
       (start! container id state-out lease)))))
