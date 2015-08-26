(ns sneer.convos
  (:require
    [clojure.core.async :refer [chan <! >!]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [go-while-let republish-latest-every!]]
    [sneer.contacts :refer [id->puk]]
    [sneer.convo :refer [convo-by-id]]
    [sneer.convo-summarization :refer :all]                 ; Force compilation of interface
    [sneer.flux :refer [tap-actions response]]
    [sneer.rx :refer [close-on-unsubscribe! pipe-to-subscriber! shared-latest]]
    [sneer.tuple.persistent-tuple-base :refer [timestamped]]
    [sneer.time :as time]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple-base-provider :refer :all]
    [sneer.interfaces])
  (:import
    [rx Subscriber Observable]
    [sneer.commons Container]
    [sneer.convos Convos Summary]
    [sneer.interfaces ConvoSummarization]
    [sneer.flux Dispatcher]
    [sneer.admin SneerAdmin]))

(defn- to-foreign-summary [pretty-time {:keys [nick preview timestamp unread id]}]
  (Summary. nick preview (pretty-time timestamp) (str unread) id))

(defn- to-foreign [summaries]
  (mapv (partial to-foreign-summary (time/pretty-printer))
        summaries))

(defn- summaries-obs* [^ConvoSummarization summarization]
  (shared-latest
    (rx/observable*
      (fn [^Subscriber subscriber]
        (let [in (.slidingSummaries summarization)
              out (chan 1 (map to-foreign))]
          (close-on-unsubscribe! subscriber in out)
          (pipe-to-subscriber! out subscriber "conversation summaries")
          (republish-latest-every! (* 60 1000) in out))))))

(defn- handle-msg-actions! [^Container container admin own-puk]
  (let [tb (tuple-base-of admin)
        contacts (sneer.contacts/from container)
        actions (chan 1)]
    (tap-actions (.produce container Dispatcher) actions)
    (go-while-let [action (<! actions)]
      (case (action :type)

        "send-message"
        (let [{:strs [contact-id text]} action
              contact-puk (<! (id->puk contacts contact-id))] ;WARNING: if the contact-id is invalid this will block FOREVER!
          (when contact-puk
            (let [tuple {"type"         "message"
                         "author"       own-puk
                         "audience"     contact-puk
                         "message-type" "chat"
                         "label"        text}]
              (store-tuple tb (timestamped tuple))))) ;TODO Create a tb function to store a new tuple, that takes care of own-puk and timestamp.

        "set-message-read"
        (let [{:strs [contact-id message-id]} action
              contact-puk (<! (id->puk contacts contact-id))
              tuple {"author" own-puk "type" "message-read" "audience" contact-puk "payload" message-id}]
          (store-tuple tb (timestamped tuple)))

        "start-session"
        (let [{:strs [contact-id session-type]} action
              contact-puk (<! (id->puk contacts contact-id))
              tuple {"type"        "session"
                     "author"       own-puk
                     "audience"     contact-puk
                     "session-type" session-type}]
          (let [session (<! (store-tuple tb (timestamped tuple)))]
            (>! (response action) (session "id"))))

        :pass))))

(defn reify-Convos [^Container container]
  (let [admin ^SneerAdmin (.produce container SneerAdmin)
        own-puk (.. admin privateKey publicKey)
        summarization ^ConvoSummarization (.produce container ConvoSummarization)
        summaries-obs (summaries-obs* summarization)
        contacts (sneer.contacts/from container)]

    (handle-msg-actions! container admin own-puk)

    (reify Convos
      (summaries [_] summaries-obs)

      (problemWithNewNickname [_ newContactNick]
        (sneer.contacts/problem-with-new-nickname contacts newContactNick))

      (startConvo [_ newContactNick]
        (sneer.contacts/new-contact contacts newContactNick))

      (acceptInvite [_ newContactNick contactPuk inviteCodeReceived]
        (sneer.contacts/accept-invite contacts newContactNick contactPuk inviteCodeReceived))

      (getById [_ id]
        (convo-by-id container id))

      (findConvo [_ inviterPuk]
        (sneer.contacts/find-convo contacts inviterPuk))

      (ownPuk [_]
        (.toHex own-puk)))))

