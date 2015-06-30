(ns sneer.convos
  (:require
    [clojure.core.async :refer [go chan close! <! >! <!! sliding-buffer alt! timeout mult]]
    [clojure.stacktrace :refer [print-stack-trace]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [close-with! sliding-chan sliding-tap
                         go-trace go-while-let go-loop-trace
                         close-on-unsubscribe! pipe-to-subscriber! republish-latest-every!]]
    [sneer.commons :refer [now produce! descending loop-trace niy]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.convo :refer [convo-by-id]]
    [sneer.convo-summarization :refer :all] ; Force compilation of interface
    [sneer.rx :refer [shared-latest]]
    [sneer.party :refer [party->puk]]
    [sneer.serialization :refer [serialize deserialize]]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple-base-provider :refer :all]
    [sneer.interfaces])
  (:import
    [java.util Date]
    [org.ocpsoft.prettytime PrettyTime]
    [rx Subscriber]
    [sneer.admin SneerAdmin]
    [sneer.commons Clock]
    [sneer.convos Convos Convos$Summary]
    [sneer.commons.exceptions FriendlyException]
    [sneer.interfaces ConvoSummarization]))

(defn- to-foreign-summary [pretty-time {:keys [nick summary timestamp unread id]}]
  (let [date (.format pretty-time (Date. ^long timestamp))]
    (Convos$Summary. nick summary date (str unread) id)))

(defn to-foreign [summaries]
  (mapv (partial to-foreign-summary (PrettyTime. (Date. (Clock/now))))
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

(defn store-contact! [container newContactNick]
  (let [admin (.produce container SneerAdmin)
        own-puk (.. admin privateKey publicKey)
        tuple-base (tuple-base-of admin)]
    (<!! (store-tuple tuple-base {"type"      "contact"
                                  "payload"   newContactNick
                                  "timestamp" (now)
                                  "audience"  own-puk
                                  "author"    own-puk}))))


(defn- start-convo! [container newContactNick]
  (let [result (rx.subjects.AsyncSubject/create)
        summarization ^ConvoSummarization (.produce container ConvoSummarization)]

    (go-trace
     (let [tuple (store-contact! container newContactNick)
           attempt-id (tuple "id")
           _ (<! (.processUpToId summarization attempt-id))
           actual-id (.getIdByNick summarization newContactNick)]
       (if (= actual-id attempt-id)
         (do
           (rx/on-next result attempt-id)
           (rx/on-completed result))
         (rx/on-error result (FriendlyException. (if actual-id (str newContactNick " was already a contact") "Unknown error"))))))

    result))

(defn reify-Convos [container]
  (let [summarization ^ConvoSummarization (.produce container ConvoSummarization)
        summaries-obs (summaries-obs* summarization)]
    (reify Convos
      (summaries [_] summaries-obs)

      (problemWithNewNickname [_ newContactNick]
        (cond
          (.isEmpty newContactNick) "cannot be empty"
          (.getIdByNick summarization newContactNick) "already used"))

      (startConvo [_ newContactNick]
        (start-convo! container newContactNick))

      (getById [_ id]
        (convo-by-id container id)))))
