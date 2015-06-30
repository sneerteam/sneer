(ns sneer.convo
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :as async :refer [<! >! chan]]
            [sneer.async :refer [pipe-to-subscriber! close-on-unsubscribe! state-machine go-trace go-loop-trace sliding-chan]]
            [sneer.chat :refer [reify-Chat]]
            [sneer.rx :refer [shared-latest]]
            [sneer.tuple.protocols :refer :all]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.persistent-tuple-base :refer [after-id]])
  (:import
    [sneer.convos Convo]
    [rx Subscriber]))

; public Convo(String nick, String inviteCodePending, Chat chat, List<SessionSummary> sessionSummaries)
; public SessionSummary(long id, String type, String title, String date, String unread)
; interface Chat { List<Message> messages(); }
; public Message(long id, String text, boolean isOwn, String date)
(defn- to-foreign [state]
  (Convo. "Maico" "" nil nil))

(defn- handle-tuple [state tuple]
  (assoc state :id (tuple "id")))

(defn- tuple-base [container]
  (tuple-base-of (.produce container sneer.admin.SneerAdmin)))

(defn- catch-up [container id]
  (let [tuples (chan)]
    (query-tuples (tuple-base container) {after-id (dec id)} tuples)
    (go-loop-trace [state {}
                    tuple (<! tuples)]
      (if (some? tuple)
        (recur (handle-tuple state tuple)
               (<! tuples))
        state))))

(defn- query-remaining-tuples [container last-id lease]
  (let [tuples (chan)]
    (query-tuples (tuple-base container) {after-id last-id} tuples lease)
    tuples))

(defn- start!
  "`id' is the id of the first contact tuple for this party"
  [container id convo-ch lease]
  (go-trace
    (let [state (<! (catch-up container id))]
      (>! convo-ch state)
      (let [taps (state-machine state handle-tuple (query-remaining-tuples container (:id state) lease))]
        (>! taps convo-ch)
        (<! lease)))))

(defn convo-by-id [container id]
  (rx/observable*
   (fn [^Subscriber subscriber]
     (let [convo-ch (sliding-chan 1 (map to-foreign))
           lease (chan)]
       (close-on-unsubscribe! subscriber convo-ch lease)
       (pipe-to-subscriber! convo-ch subscriber "convo")
       (start! container id convo-ch lease)))))
