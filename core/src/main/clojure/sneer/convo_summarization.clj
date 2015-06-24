(ns sneer.convo-summarization
  (:require
    [clojure.core.async :refer [go chan close! <! >! <!! >!! sliding-buffer alt! timeout mult]]
    [clojure.stacktrace :refer [print-stack-trace]]
    [sneer.async :refer [close-with! sliding-chan sliding-tap go-while-let go-trace go-loop-trace close-on-unsubscribe! pipe-to-subscriber! state-machine tap-state peek-state]]
    [sneer.commons :refer [now produce! descending loop-trace niy]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.conversation :refer :all]
    [sneer.io :as io]
    [sneer.rx :refer [shared-latest]]
    [sneer.party :refer [party->puk]]
    [sneer.serialization :refer [serialize deserialize]]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.persistent-tuple-base :as tb]
    [sneer.interfaces])
  (:import
    [java.io File]
    [sneer.admin SneerAdmin]
    [sneer.commons PersistenceFolder]
    [sneer.interfaces ConvoSummarization]))

(defn- contact-puk [tuple]
  (tuple "party"))

(defn- handle-contact [own-puk state tuple]
  (if-not (= (tuple "author") own-puk)
    state
    (let [new-nick (tuple "payload")
          nick-already-used? (get-in state [:nick->summary new-nick])]
      (if nick-already-used?
        state
        (let [puk (contact-puk tuple)
              old-nick (get-in state [:puk->nick puk])
              summary  (get-in state [:nick->summary old-nick])
              summary  (or summary {:id (tuple "id")})
              summary  (assoc summary :nick new-nick
                                      :timestamp (tuple "timestamp"))]
          (-> state
            (update-in [:nick->summary] dissoc old-nick)
            (assoc-in  [:nick->summary new-nick] summary)
            (assoc-in  [:puk->nick puk] new-nick)))))))

(defn- unread-status [label old-status]
  (cond
    (= old-status "?") "?"
    (.contains label "?") "?"
    :else "*"))

(defn- update-with-received [id label old-summary]
  (-> old-summary
    (assoc :last-received id)
    (update :unread #(unread-status label %))))

(defn- update-summary [own? message old-summary]
  (let [label     (message "label")
        timestamp (message "timestamp")]
    (->
      (if own?
        old-summary
        (update-with-received (message "original_id") label old-summary))
      (assoc
        :preview (or label "")
        :timestamp timestamp))))

(defn- handle-message [own-puk state message]
  (let [author (message "author")
        own? (= author own-puk)
        contact-puk (if own? (message "audience") author)]
    (if-let [nick (get-in state [:puk->nick contact-puk])]
      (update-in state [:nick->summary nick] #(update-summary own? message %))
      state)))

(defn- update-with-read [summary tuple]
  (let [msg-id (tuple "payload")]
    (cond-> summary
      (= msg-id (:last-received summary)) (assoc :unread ""))))

(defn- handle-read-receipt [own-puk state tuple]
  (let [contact-puk (tuple "audience")
        nick (get-in state [:puk->nick contact-puk])]
    (cond-> state
      (= (tuple "author") own-puk)
      (update-in [:nick->summary nick]
                 update-with-read tuple))))

(defn handle-tuple [own-puk state tuple]
  (->
   (case (tuple "type")
     "contact"      (handle-contact      own-puk state tuple)
     "message"      (handle-message      own-puk state tuple)
     "message-read" (handle-read-receipt own-puk state tuple)
     state)
   (assoc :last-id (tuple "id"))))

(defn- summarization-loop! [previous-state own-puk tuples]
  (let [previous-state (or previous-state {:last-id 0})]
    (state-machine previous-state (partial handle-tuple own-puk) tuples)))

; state: {:last-id long
;         :puk->nick {puk "Neide"}
;         :nick->summary {"Neide" {:nick "Neide"
;                                  :timestamp long
;                                  :unread "*"
;                                  :preview "Hi, Maico"
;                                  :id long}}
(defn- start-summarization-machine! [container previous-state]
  (let [lease (.produce container :lease)
        admin (.produce container SneerAdmin)
        own-puk (.. admin privateKey publicKey)
        tuple-base (tuple-base-of admin)
        tuples (chan)
        all-tuples-criteria (if (some? previous-state)
                              {tb/after-id (previous-state :last-id)}
                              {})]

    (query-tuples tuple-base all-tuples-criteria tuples lease)
    (close-with! lease tuples)

    (summarization-loop! previous-state own-puk tuples)))

(defn- read-snapshot [file]
  (when (and file (.exists file))
    (try
      (deserialize (io/read-bytes file))
      (catch Exception e
        (println "Exception reading snapshot:" (.getMessage e))
        nil))))

(defn- write-snapshot [file snapshot]
  (when file
    (io/write-bytes file (serialize snapshot))
    (println "Snapshot written:" (-> snapshot count dec)    ; dec: Don't count the :last-id key.
             "conversations, last-id:" (:last-id snapshot))))

(defn- start-saving-snapshots-to! [file ch]
  (let [never (chan)]
    (go-loop-trace [snapshot nil
                    debounce never]
      (alt! :priority true
        ch       ([snapshot]
                   (when snapshot
                     (recur snapshot (timeout 5000))))
        debounce ([_]
                   (write-snapshot file snapshot)
                   (recur nil never))))))

(defn- start-machine! [container]
  (let [file (some-> (.produce container PersistenceFolder)
                     (.get)
                     (File. "conversation-summaries.tmp"))
        previous-state (read-snapshot file)
        machine (start-summarization-machine! container previous-state)]
    (start-saving-snapshots-to! file (tap-state machine))
    machine))

(defn- nick->id [state nick]
  (get-in state [:nick->summary nick :id]))

(def ^:private xsummarize (comp (map :nick->summary)
                                (dedupe)
                                (map vals)
                                (map #(sort-by :timestamp descending %))
                                (map vec)))

(defn sliding-summaries!
  ([own-puk tuples-in]
   (sliding-summaries! (summarization-loop! nil own-puk tuples-in)))
  ([machine]
   (tap-state machine (sliding-chan 1 xsummarize))))

(defn reify-ConvoSummarization [container]
  (let [machine (start-machine! container)
        sliding-summaries (sliding-summaries! machine)]
    (reify ConvoSummarization

      (slidingSummaries [_] sliding-summaries)

      (getIdByNick
       [_ nick]
       (nick->id (<!! (peek-state machine)) nick))

      (processUpToId
       [_ id]
       (let [state (tap-state machine)]
         (go-trace
           (loop []
             (let [current (<! state)
                   last-id (current :last-id)]
               (when (< last-id id)
                 (recur))))
           (close! state)
           nil))))))
