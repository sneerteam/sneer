(ns sneer.convo-summarization
  (:require
    [clojure.core.async :as async :refer [go chan close! <! >! <!! >!! sliding-buffer alt! timeout mult]]
    [clojure.stacktrace :refer [print-stack-trace]]
    [sneer.async :refer [close-with! sliding-chan sliding-tap go-while-let go-trace go-loop-trace close-on-unsubscribe! pipe-to-subscriber!]]
    [sneer.commons :refer [now produce! descending loop-trace niy]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.conversation :refer :all]
    [sneer.flux :as flux]                                ; Required to cause compilation of LeaseHolder
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
    [sneer.flux LeaseHolder]
    [sneer.commons PersistenceFolder]
    [sneer.interfaces ConvoSummarization]))

(defn- contact-puk [tuple]
  (tuple "party"))

(defn- handle-contact [own-puk tuple state]
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

(defn- handle-message [own-puk message state]
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

(defn- handle-read-receipt [own-puk tuple state]
  (let [contact-puk (tuple "audience")
        nick (get-in state [:puk->nick contact-puk])]
    (cond-> state
      (= (tuple "author") own-puk)
      (update-in [:nick->summary nick]
                 update-with-read tuple))))

(defn- summarization-loop [previous-state own-puk tuples state-out]
  (go-loop-trace [state previous-state]
    (>! state-out state)
    (when-let [tuple (<! tuples)]
      (recur
       (->
        (case (tuple "type")
          "contact" (handle-contact own-puk tuple state)
          "message" (handle-message own-puk tuple state)
          "message-read" (handle-read-receipt own-puk tuple state)
          state)
        (assoc :last-id (tuple "id")))))))

; state: {:last-id long
;         :puk->nick {puk "Neide"}
;         :nick->summary {"Neide" {:nick "Neide"
;                                  :timestamp long
;                                  :unread "*"
;                                  :preview "Hi, Maico"
;                                  :id long}}
(defn start-summarization-machine!
  ([container previous-state state-out]
    (let [lease (.getLeaseChannel (.produce container LeaseHolder))
          admin (.produce container SneerAdmin)
          own-puk (.. admin privateKey publicKey)
          tuple-base (tuple-base-of admin)]
      (start-summarization-machine! previous-state own-puk tuple-base state-out lease)))

  ([previous-state own-puk tuple-base state-out lease]      ; Called by the summarization test.
   (let [last-id (previous-state :last-id)
         tuples (chan)
         all-tuples-criteria {tb/after-id last-id}]

     (query-tuples tuple-base all-tuples-criteria tuples lease)
     (close-with! lease tuples)

     (summarization-loop previous-state own-puk tuples state-out))))

(defn- read-snapshot [file]
  (let [default {}]
    (if (and file (.exists file))
      (try
        (deserialize (io/read-bytes file))
        (catch Exception e
          (println "Exception reading snapshot:" (.getMessage e))
          default))
      default)))

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

(defn- tap-state
  ([machine ch]
   (>!! ch @(machine :state)) ; TODO: make it deterministic by sending messages to the machine
   (async/tap (machine :state-mult) ch)
   ch)
  ([machine]
   (sliding-tap (machine :state-mult))))

(defn- start-machine! [container]
  (let [file (some-> (.produce container PersistenceFolder)
                     (.get)
                     (File. "conversation-summaries.tmp"))
        state (atom (read-snapshot file))
        state-out (sliding-chan)
        machine {:state      state
                 :state-mult (mult state-out)}]

    (start-saving-snapshots-to! file (tap-state machine))

    (go-while-let [current (<! (tap-state machine))]
      (reset! state current))

    (start-summarization-machine! container @state state-out)

    machine))

(defn- nick->id [state nick]
  (get-in state [:nick->summary nick :id]))

(def ^:private xsummarize (comp (map :nick->summary)
                                (dedupe)
                                (map vals)
                                (map #(sort-by :timestamp descending %))
                                (map vec)))

(defn sliding-summaries! [own-puk tuples-in]
  (let [summaries-out (sliding-chan 1 xsummarize)
        loop (summarization-loop {} own-puk tuples-in summaries-out)]
    (go-trace (<! loop)
              (close! summaries-out))
    summaries-out))

(defn reify-ConvoSummarization [container]
  (let [machine (start-machine! container)
        state (:state machine)]
    (reify ConvoSummarization

      (slidingSummaries
       [_]
       (let [summaries-out (sliding-chan 1 xsummarize)]
         (tap-state machine summaries-out)
         summaries-out))

      (getIdByNick
       [_ nick]
       (nick->id @state nick))

      (processUpToId
       [_ id]
       (let [state (tap-state machine)]
         (go-trace
           (loop []
             (let [current (<! state)
                   last-id (or (current :last-id) 0)] ; TODO: move :last-id initialization to start-machine!
               (when (< last-id id)
                 (recur))))
           (close! state)
           nil))))))
