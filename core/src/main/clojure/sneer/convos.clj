(ns sneer.convos
  (:require
    [clojure.core.async :refer [go chan close! <! >! <!! sliding-buffer timeout mult]]
    [clojure.stacktrace :refer [print-stack-trace]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [close-with! sliding-chan sliding-tap
                         go-trace go-while-let go-loop-trace
                         republish-latest-every!]]
    [sneer.commons :refer [now produce! descending loop-trace niy]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.convo :refer [convo-by-id]]
    [sneer.convo-summarization :refer :all] ; Force compilation of interface
    [sneer.rx :refer [close-on-unsubscribe! pipe-to-subscriber! shared-latest]]
    [sneer.party :refer [party->puk]]
    [sneer.serialization :refer [serialize deserialize]]
    [sneer.time :as time]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple-base-provider :refer :all]
    [sneer.interfaces])
  (:import
    [rx Subscriber]
    [sneer.commons Container]
    [sneer.convos Convos Summary]
    [sneer.interfaces ConvoSummarization]))

(defn- to-foreign-summary [pretty-time {:keys [nick summary timestamp unread id]}]
  (Summary. nick summary (pretty-time timestamp) (str unread) id))

(defn to-foreign [summaries]
  (mapv (partial to-foreign-summary (time/pretty-printer))
        summaries))

(defn- summaries-obs* [summarization]
  (shared-latest
    (rx/observable*
      (fn [^Subscriber subscriber]
        (let [in (.slidingSummaries summarization)
              out (chan 1 (map to-foreign))]
          (close-on-unsubscribe! subscriber in out)
          (pipe-to-subscriber!   out subscriber "conversation summaries")
          (republish-latest-every! (* 60 1000) in out))))))

(defn reify-Convos [^Container container]
  (let [summarization ^ConvoSummarization (.produce container ConvoSummarization)
        summaries-obs (summaries-obs* summarization)
        contacts (sneer.contacts/from container)]
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
