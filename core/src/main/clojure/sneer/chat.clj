(ns sneer.chat
  #_(:require
    [clojure.core.async :refer [go chan]])
  (:import
    [sneer.convos Chat]))

(defn reify-Chat [contact-puk]
  (reify Chat

    ;Observable<List<Message>>
    (messages [_])

    (sendMessage [_ text])                                  ;

    (setMessageRead [_ msg-id])))
