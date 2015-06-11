(ns sneer.convo
  (:require [rx.lang.clojure.core :as rx]
            [sneer.async :refer [pipe-to-subscriber!]]
            [sneer.chat :refer [reify-Chat]]
            [sneer.rx :refer [shared-latest]])
  (:import
    [sneer.convos Convo]
    [rx Subscriber]))

(defn nick-obs* [nick-ch]
  (shared-latest
    (rx/observable*
      (fn [^Subscriber subscriber]
        (pipe-to-subscriber! nick-ch subscriber "convo nick")))))

(defn reify-Convo [contact-puk nick-ch]
  (let [nick-obs (nick-obs* nick-ch)]
    (reify Convo
      (nick [_] nick-obs)

      ;/** @return null if invite already accepted
      ;*/Observable<String> inviteCodePending ()
      (inviteCodePending [_])

      (chat [_]
        (reify-Chat contact-puk))

      ;Observable<List<SessionSummary>>
      (sessionSummaries [_]))))
