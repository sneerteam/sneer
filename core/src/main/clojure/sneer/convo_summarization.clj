(ns sneer.convo-summarization
  (:require
    [clojure.core.async :as async :refer [go chan close! <! >! <!! >!! sliding-buffer alt! timeout mult]]
    [clojure.stacktrace :refer [print-stack-trace]]
    [sneer.async :refer [close-with! sliding-chan sliding-tap go-while-let go-trace go-loop-trace state-machine tap-state peek-state! debounce]]
    [sneer.commons :refer [now produce! descending loop-trace niy]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.conversation :refer :all]
    [sneer.io :as io]
    [sneer.message-subs]
    [sneer.rx :refer [close-on-unsubscribe! pipe-to-subscriber! shared-latest]]
    [sneer.party :refer [party->puk]]
    [sneer.serialization :refer [serialize deserialize]]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.persistent-tuple-base :as tb]
    [sneer.interfaces])
  (:import
    [java.io File]
    [sneer.admin SneerAdmin]
    [sneer.commons Container PersistenceFolder]
    [sneer.interfaces ConvoSummarization]))

(defn- contact-puk [tuple]
  (tuple "party"))

(defn- handle-contacts [own-puk state event]
  (assoc state :contacts (event :state)))

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
    (update-in state
               [:puk->summary contact-puk]
               #(update-summary own? message %))))

(defn- update-with-read [summary tuple]
  (let [msg-id (tuple "payload")]
    (cond-> summary
      (= msg-id (:last-received summary)) (assoc :unread ""))))

(defn- handle-read-receipt [own-puk state tuple]
  (let [contact-puk (tuple "audience")]
    (cond-> state
      (= (tuple "author") own-puk)
      (update-in [:puk->summary contact-puk]
                 update-with-read tuple))))

(defn handle-event [own-puk state event]
  (let [type (event "type")]
    (if (= type :contacts)
      (handle-contacts own-puk state event)
      (->
       (case type
         "message"      (handle-message      own-puk state event)
         "message-read" (handle-read-receipt own-puk state event)
         state)
       (assoc :last-id (event "id"))))))

(defn- summarization-loop! [previous-state own-puk events]
  (let [previous-state (or previous-state {:last-id 0})]
    (state-machine (partial handle-event own-puk) previous-state events)))

;; state: {:last-id long
;;         :puk->summary {NeidePuk {:timestamp long
;;                                  :preview "Hi, Maico"
;;                                  :unread "*"
;;                                  :last-received original_id}}
(defn- start-summarization-machine! [^Container container previous-state]
  (let [lease (.produce container :lease)
        admin ^SneerAdmin (.produce container SneerAdmin)
        own-puk (.. admin privateKey publicKey)
        tuple-base (tuple-base-of admin)
        tuples (chan)
        all-tuples-criteria (if (some? previous-state)
                              {tb/after-id (previous-state :last-id)}
                              {})

        contacts (sneer.contacts/from container)
        contacts-updates (sneer.contacts/tap contacts (chan 1 (map #(do {"type" :contacts :state %}))))]

    (sneer.message-subs/from container)
    (query-tuples tuple-base all-tuples-criteria tuples lease)
    (close-with! lease tuples)
    (summarization-loop! previous-state own-puk (async/merge [tuples contacts-updates]))))

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
  (let [snapshots (chan)]
    (debounce ch snapshots 5000)
    (go-while-let [snapshot (<! snapshots)]
      (write-snapshot file snapshot))))

(defn- start-machine! [^Container container]
  (let [file (some-> (.produce container PersistenceFolder)
                     (.get)
                     (File. "conversation-summaries.tmp"))
        previous-state (read-snapshot file)
        machine (start-summarization-machine! container previous-state)]
    (start-saving-snapshots-to! file (tap-state machine))
    machine))

;; State -> [{:id :nick :timestamp :summary}]
(defn -summarize [{:keys [puk->summary contacts]}]
  (->>
   contacts
   sneer.contacts/contact-list
   (map
    (fn [{:keys [id puk nick timestamp] :as contact}]
      (if-some [summary (get puk->summary puk)]
        (assoc summary
               :id id
               :nick nick
               :timestamp (max timestamp (or (summary :timestamp) 0)))
        (assoc contact :preview "" :unread ""))))
   (sort-by :timestamp descending)
   vec))

(def ^:private xsummarize (comp (dedupe)
                                (map -summarize)))

(defn sliding-summaries!
  ([own-puk tuples-in]
   (sliding-summaries! (summarization-loop! nil own-puk tuples-in)))
  ([machine]
   (tap-state machine (sliding-chan 1 xsummarize))))

(defn reify-ConvoSummarization [container]
  (let [machine (start-machine! container)]
    (reify ConvoSummarization

      (slidingSummaries [_]
        (sliding-summaries! machine))

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
