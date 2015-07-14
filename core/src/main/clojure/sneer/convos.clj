(ns sneer.convos
  (:require
    [clojure.core.async :refer [chan <!]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [go-while-let republish-latest-every!]]
    [sneer.contacts :refer [id->puk]]
    [sneer.convo :refer [convo-by-id]]
    [sneer.convo-summarization :refer :all]                 ; Force compilation of interface
    [sneer.flux :refer [tap-actions]]
    [sneer.rx :refer [close-on-unsubscribe! pipe-to-subscriber! shared-latest]]
    [sneer.tuple.persistent-tuple-base :refer [timestamped]]
    [sneer.time :as time]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple-base-provider :refer :all]
    [sneer.interfaces])
  (:import
    [rx Subscriber Observable]
    [sneer.commons Container]
    [sneer.convos Convos Summary Notifications]
    [sneer.interfaces ConvoSummarization]
    [sneer.flux Dispatcher]
    [sneer.admin SneerAdmin]))

(defn- to-foreign-summary [pretty-time {:keys [nick summary timestamp unread id]}]
  (Summary. nick summary (pretty-time timestamp) (str unread) id))

(defn- to-foreign [summaries]
  (mapv (partial to-foreign-summary (time/pretty-printer))
        summaries))

(defn- summaries-obs* [summarization]
  (shared-latest
    (rx/observable*
      (fn [^Subscriber subscriber]
        (let [in (.slidingSummaries summarization)
              out (chan 1 (map to-foreign))]
          (close-on-unsubscribe! subscriber in out)
          (pipe-to-subscriber! out subscriber "conversation summaries")
          (republish-latest-every! (* 60 1000) in out))))))

(defn- handle-msg-actions! [container]
  (let [admin (.produce container SneerAdmin)
        tb (tuple-base-of admin)
        own-puk (.. admin privateKey publicKey)
        contacts (sneer.contacts/from container)
        actions (chan 1)]
    (tap-actions (.produce container Dispatcher) actions)
    (go-while-let [action (<! actions)]
      (when (= (action :type) "send-message")
        (let [{:strs [contact-id text]} action
              contact-puk (<! (id->puk contacts contact-id))]
          (when contact-puk
            (let [tuple {"type"         "message"
                         "author"       own-puk
                         "audience"     contact-puk
                         "message-type" "chat"
                         "label"        text}]
              (store-tuple tb (timestamped tuple)))))))))   ;TODO Create a tb function to store a new tuple, that takes care of own-puk and timestamp.

(defn reify-Convos [^Container container]
  (let [summarization ^ConvoSummarization (.produce container ConvoSummarization)
        summaries-obs (summaries-obs* summarization)
        contacts (sneer.contacts/from container)]

    (handle-msg-actions! container)

    (reify Convos
      (summaries [_] summaries-obs)

      (problemWithNewNickname [_ newContactNick]
        (sneer.contacts/problem-with-new-nickname contacts newContactNick))

      (startConvo [_ newContactNick]
        (sneer.contacts/new-contact contacts newContactNick))

      (acceptInvite [_ newContactNick contactPuk inviteCodeReceived]
        (sneer.contacts/accept-invite contacts newContactNick contactPuk inviteCodeReceived))

      (getById [_ id]
        (convo-by-id container id)))))

(defn reify-Notifications [^Container container]
  (reify Notifications
    (get [_]
      (Observable/just nil))

    (startIgnoring [_ convoId]
      )

    (stopIgnoring [_]
      )))
