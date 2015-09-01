(ns sneer.sessions
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :refer [go chan <! <!! close!]]
            [sneer.async :refer [go-while-let]]
            [sneer.rx :refer [close-on-unsubscribe! pipe-to-subscriber!]]
            [sneer.flux :refer [tap-actions]]
            [sneer.tuple.protocols :refer :all]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.persistent-tuple-base :refer [timestamped]]
            [sneer.queries :refer :all])
  (:import [sneer.commons Container]
           [sneer.flux Dispatcher]
           [sneer.admin SneerAdmin]
           [sneer.convos Sessions SessionMessage]
           [sneer.rx Timeline]
           [rx Subscriber]))

(defn- produce [^Container c class]
  (.produce c class))

(defn- puk [^SneerAdmin admin]
  (.. admin privateKey publicKey))

(defn- ->observable [ch label]
  (rx/observable*
   (fn [^Subscriber subscriber]
     (close-on-unsubscribe! subscriber ch)
     (pipe-to-subscriber! ch subscriber label))))

(def ^:private no-messages (doto (chan) close!))

(defn- query-session-tuple [tuple-base session-id]
  (let [session-tuples (chan 1)]
    (query-tuples tuple-base {"id" session-id "type" "session"} session-tuples)
    session-tuples))

(defn ->SessionMessage [own-puk {:strs [author payload] :as t}]
  (SessionMessage. payload (= own-puk author)))

(defn- query-session-messages [own-puk tuple-base session-id lease]
  (go
    (if-some [{:strs [author audience]} (<! (query-session-tuple tuple-base session-id))]
      (let [criteria {"type" "session-message" "session-id" session-id}
            xform    (map #(->SessionMessage own-puk %))]
        (query-convo-tuples tuple-base criteria author audience lease xform))
      [no-messages no-messages])))

(defn- handle-send-session-message [own-puk tuple-base {:strs [session-id payload]}]
  (go
    (when-some [{:strs [author audience]} (<! (query-session-tuple tuple-base session-id))]
      (let [tuple {"type"         "session-message"
                   "session-id"   session-id
                   "author"       own-puk
                   "audience"     (if (= own-puk author) audience author)
                   "payload"      payload}]
        (store-tuple tuple-base (timestamped tuple))))))

(defn- handle-actions! [own-puk dispatcher tuple-base]
  (let [actions (chan 1)]
    (tap-actions dispatcher actions)
    (go-while-let [action (<! actions)]
      (case (:type action)
        "send-session-message"
        (<! (handle-send-session-message own-puk tuple-base action))
        :pass))))

(defn reify-Sessions [container]
  (let [dispatcher (produce container Dispatcher)
        admin      (produce container SneerAdmin)
        own-puk    (puk admin)
        tuple-base (tuple-base-of admin)
        lease      (produce container :lease)]
    (handle-actions! own-puk dispatcher tuple-base)
    (reify Sessions
      (messages [_ session-id]
        (let [[past future] (<!! (query-session-messages own-puk tuple-base session-id lease))]
          (Timeline. (->observable past "past")
                     (->observable future "future")))))))
