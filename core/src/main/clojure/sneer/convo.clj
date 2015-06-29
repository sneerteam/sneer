(ns sneer.convo
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :as async :refer [<! >! chan]]
            [sneer.async :refer [state-machine go-trace go-loop-trace sliding-chan]]
            [sneer.contacts]
            [sneer.rx :refer [pipe-to-subscriber! close-on-unsubscribe! shared-latest]]
            [sneer.tuple.protocols :refer :all]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.persistent-tuple-base :refer [after-id]])
  (:import
    [sneer.convos Convo]
    [rx Subscriber]
    [sneer.admin SneerAdmin]))

(defn- update-nick [state tuple]
  (some->> (tuple "payload")
           (assoc state :nick)))

(defn- update-contact-with-puk [state tuple]
  (if (= (state :puk) (tuple "party"))
    (update-nick state tuple)
    state))

(defn handle-invite-acceptance [state tuple]
  (let [puk (tuple "party")]
    (-> state
        (assoc :puk puk)
        (dissoc :invite-code))))

(defn- update-contact-without-puk [state tuple]
  (if (= (state :id) (tuple "id"))
    (-> state
      (update-nick tuple)
      (assoc :invite-code (tuple "invite-code")))
    (if (= (state :nick) (tuple "payload"))
      (handle-invite-acceptance state tuple)
      state)))

(defn- handle-contact [state tuple]
  (if (contains? state :puk)
    (update-contact-with-puk state tuple)
    (update-contact-without-puk state tuple)))

(defn- handle-tuple [state tuple]
  (->
   (case (tuple "type")
     "contact" (handle-contact state tuple)
     state)
   (assoc :last-id (tuple "id"))))

(defn- tuple-base [container]
  (tuple-base-of (.produce container SneerAdmin)))

(defn- query-convo-tuples [container starting-id tuples]
  ; contact, push (invite accept), message
  (query-tuples (tuple-base container) {after-id starting-id} tuples))

(defn- catch-up [container id]
  (let [tuples (chan)]
    (query-convo-tuples container (dec id) tuples)
    (async/reduce handle-tuple {:id id} tuples)))

(defn- query-remaining-tuples [container last-id lease]
  (let [tuples (chan)]
    (query-tuples (tuple-base container) {after-id last-id} tuples lease)
    tuples))

(defn- start!
  "`id' is the id of the first contact tuple for this party"
  [container id state-out lease]
  (go-trace
    ; don't emit historical snapshots upon startup
    (let [state (<! (catch-up container id))]
      (>! state-out state)
      (let [tuples (query-remaining-tuples container (:last-id state) lease)
            taps (state-machine handle-tuple state tuples)]
        (>! taps state-out)
        (<! lease)))))

; public Convo(long id, String nick, String inviteCodePending, Chat chat, List<SessionSummary> sessionSummaries)
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
