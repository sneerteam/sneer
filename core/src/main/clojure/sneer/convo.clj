(ns sneer.convo
  #_(:require
    [clojure.core.async :refer [go chan]])
  (:import
    [sneer.convos Convo]))

(defn reify-Convo [id]
  (reify Convo
    ;Observable<String> nick();
    (nick [_])

    ;/** @return null if invite already accepted
    ;*/Observable<String> inviteCodePending ()
    (inviteCodePending [_])

    ;Chat chat ()
    (chat [_])

    ;Observable<List<SessionSummary>>
    (sessionSummaries [_])))
