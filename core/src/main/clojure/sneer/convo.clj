(ns sneer.convo

  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :as async :refer [<! >! chan alts! pipe]]
            [sneer.async :refer [state-machine tap-state go-trace go-loop-trace sliding-chan close-with!]]
            [sneer.contacts :as contacts :refer [tap-id]]
            [sneer.rx :refer [pipe-to-subscriber! close-on-unsubscribe!]]
            [sneer.time :as time]
            [sneer.tuple.protocols :refer :all]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.persistent-tuple-base :refer [after-id]])

  (:import  [rx Subscriber]
            [sneer.commons Container]
            [sneer.convos Convo ChatMessage SessionSummary]
            [sneer.admin SneerAdmin]))

(defn- msg-ids [msg1 msg2]
  (compare (msg1 :id)
           (msg2 :id)))

(defn- handle-message [own-puk state message]
  (let [{:strs [original_id author timestamp label]} message
        own? (= author own-puk)
        message {:id original_id :own? own? :timestamp timestamp :text label}]
    (update state :messages conj message)))

(defn- handle-contact [state contact]
  (merge state contact))

(defn- handle-session [own-puk state {:strs [original_id session-type timestamp]}]
  (update-in state
             [:sessions]
             (fnil conj [])
             {:id original_id :type session-type :timestamp timestamp}))

(defn- handle-event [own-puk state event]
  (case (event "type")
    "message" (handle-message own-puk state event)
    "session" (handle-session own-puk state event)
    (handle-contact state event)))

(defn- query-messages-by! [tuple-base author-puk audience-puk lease]
  (let [old (chan 1)
        new (chan 1)
        criteria {"type"     "message"
                  "author"   author-puk
                  "audience" audience-puk}]
    (query-with-history tuple-base criteria old new lease)
    [old new]))

(defn- query-messages! [tb own-puk contact-puk lease]
  (let [[old-sent new-sent] (query-messages-by! tb own-puk contact-puk lease)
        [old-rcvd new-rcvd] (query-messages-by! tb contact-puk own-puk lease)
        old-msgs (async/merge [old-sent old-rcvd])
        new-msgs (async/merge [new-sent new-rcvd])]
    [old-msgs new-msgs]))

(defn- query-session-tuples [tb author audience tuples-out lease]
  (let [criteria {"type"     "session"
                  "author"   author
                  "audience" audience}]
    (query-tuples tb criteria tuples-out lease)))

(defn- query-sessions! [tb own-puk contact-puk lease]
  (let [tuples-out (chan 1)]
    (query-session-tuples tb own-puk contact-puk tuples-out lease)
    (query-session-tuples tb contact-puk own-puk tuples-out lease)
    tuples-out))

(defn- pipe-until-contact-has-puk! [contact-in state-out]
  (go-loop-trace []
    (when-let [contact (<! contact-in)]
      (if (contact :puk)
        contact
        (do
          (>! state-out contact)
          (recur))))))

(defn- start!
  "`id' is the id of the first contact tuple for this party"
  [^Container container id state-out lease]
  (let [admin ^SneerAdmin (.produce container SneerAdmin)
        own-puk (.. admin privateKey publicKey)
        tb (tuple-base-of admin)
        contact-in (tap-id (contacts/from container) id lease)]
    (go-trace
      (when-let [contact (<! (pipe-until-contact-has-puk! contact-in state-out))]
        (let [state (assoc contact :messages (sorted-set-by msg-ids))
              [old-events new-events] (query-messages! tb own-puk (contact :puk) lease)
              sessions-in (query-sessions! tb own-puk (contact :puk) lease)
              _ (pipe contact-in new-events)
              _ (pipe sessions-in new-events)
              machine (state-machine (partial handle-event own-puk) state old-events new-events)]
          (tap-state machine state-out))))))

(defn- ->ChatMessageList [messages pretty-time]
  (mapv (fn [{:keys [id text own? timestamp]}]
          (ChatMessage. id text own? (pretty-time timestamp)))
        messages))

(defn- ->SessionSummaryList [sessions pretty-time]
  (mapv (fn [{:keys [id type timestamp]}]
          (SessionSummary. id type type (pretty-time timestamp) ""))
        sessions))

; Convo(long contactId, String nickname, String inviteCodePending, List<ChatMessage> messages, List<SessionSummary> sessionSummaries)
; SessionSummary(long id, String type, String title, String date, String unread)
; ChatMessage(long id, String text, boolean isOwn, String date)
(defn- to-foreign [{:keys [id nick invite-code messages sessions]}]
  (let [pretty-time (time/pretty-printer)]
    (Convo. id nick invite-code
            (->ChatMessageList messages pretty-time)
            (->SessionSummaryList sessions pretty-time))))

(defn convo-by-id [container id]
  (rx/observable*
   (fn [^Subscriber subscriber]
     (let [state-out (sliding-chan 1 (map to-foreign))
           lease (chan)]
       (close-on-unsubscribe! subscriber state-out lease)
       (pipe-to-subscriber! state-out subscriber "convo")
       (start! container id state-out lease)))))
