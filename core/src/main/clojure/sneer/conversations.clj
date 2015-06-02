(ns sneer.conversations
  (:require
    [clojure.core.async :as async :refer [go chan close! <! >! sliding-buffer alt! timeout]]
    [clojure.java.io :as io]
    [clojure.stacktrace :refer [print-stack-trace]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [sliding-chan go-while-let go-loop-trace link-chan-to-subscriber thread-chan-to-subscriber]]
    [sneer.commons :refer [now produce! descending]]
    [sneer.clojure.core :refer [nvl]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.conversation :refer :all]
    [sneer.rx :refer [atom->observable subscribe-on-io latest shared-latest combine-latest switch-map behavior-subject]]
    [sneer.party :refer [party->puk]]
    [sneer.serialization :refer [serialize deserialize]]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple.space :refer [payload]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.persistent-tuple-base :as tb])
  (:import
    [rx Subscriber Observable]
    [sneer Conversations Conversation Conversations$Notification]
    [sneer.admin SneerAdmin]
    [sneer.commons Container Clock PersistenceFolder]
    [sneer.conversations ConversationList ConversationList$Summary ConversationList]
    [sneer.rx ObservedSubject]
    [org.ocpsoft.prettytime PrettyTime]
    [java.io File]
    [java.util HashMap Date]))

(defn- contact-puk [tuple]
  (tuple "party"))

(defn- handle-contact! [own-puk tuple state]
  (when (= (tuple "author") own-puk)
    (when-let [contact-puk (contact-puk tuple)]
      (update-in state
                 [contact-puk]
                 assoc :name      (tuple "payload")
                       :timestamp (tuple "timestamp")))))

(defn- unread-status [label old-status]
  (cond
    (= old-status "?") "?"
    (.contains label "?") "?"
    :else "*"))

(defn update-with-received [id label old-summary]
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

(defn- handle-message! [own-puk message state]
  (let [author (message "author")
        own? (= author own-puk)
        contact-puk (if own? (message "audience") author)]
    (update-in state
               [contact-puk]
               #(update-summary own? message %))))

(defn update-with-read [old-summary tuple]
  (let [msg-id (tuple "payload")]
    (cond-> old-summary
      (= msg-id (:last-received old-summary)) (assoc :unread ""))))

(defn- handle-msg-read! [own-puk tuple state]
  (when (= (tuple "author") own-puk)
    (let [contact-puk (tuple "audience")]
      (update-in state
                 [contact-puk]
                 update-with-read tuple))))

(defn summarize [state]
  (let [pretty-time (PrettyTime. (Date. (Clock/now)))
        with-pretty-date (fn [summary]
                           (assoc summary :date
                                          (.format pretty-time (Date. ^long (summary :timestamp)))))]
    (->> state
         vals
         (filter :name)
         (sort-by :timestamp descending)
         (mapv with-pretty-date))))

(defn link-lease-to-chan
  "Closes `linked' when `lease' emits a value."
  [lease linked]
  (go
    (<! lease)
    (close! linked)))

(defn start-summarization-machine! [state own-puk tuple-base summaries-out lease]
  (let [last-id (:last-id state)
        tuples (chan)
        all-tuples-criteria {tb/after-id last-id}]
    (query-tuples tuple-base all-tuples-criteria tuples lease)

    ; link machine lifetime to lease
    (link-lease-to-chan lease tuples)

    (go-loop-trace [state state]
      (>! summaries-out state)

      (when-let [tuple (<! tuples)]
        (recur
         (->
          (case (tuple "type")
            "contact"      (handle-contact!  own-puk tuple state)
            "message"      (handle-message!  own-puk tuple state)
            "message-read" (handle-msg-read! own-puk tuple state)
            state)
          (assoc :last-id (tuple "id"))))))))


; Java interface

(defn to-foreign-summary [{:keys [name summary date unread]}]
  (println "TODO: CONVERSATION ID")
  (ConversationList$Summary. name summary date (str unread) -4242))

(defn- to-foreign [summaries]
  (->> summaries summarize (mapv to-foreign-summary)))

(defn republish-latest-every [period in out]
  (go-loop-trace [latest nil
                  period-timeout (timeout period)]
    (alt! :priority true
          in
          ([latest]
           (when latest
             (>! out latest)
             (recur latest period-timeout)))

          period-timeout
          ([_]
           (when latest
             (>! out latest))
           (recur latest (timeout period))))))

(defn read-bytes [^File file]
  (let [buffer (byte-array (.length file))]
    (.read (io/input-stream file) buffer)
    buffer))

(defn read-snapshot [file]
  (try
    (deserialize (read-bytes file))
    (catch Exception e
      (println (.getMessage e))
      {})))

(defn write-bytes [file buffer]
  (with-open [out (io/output-stream file)]
    (.write out buffer)))

(defn write-snapshot [file snapshot]
  (write-bytes file (serialize snapshot)))

(defn- save-snapshots-to [file ch]
  (go-while-let [snapshot (<! ch)]
    (write-snapshot file snapshot)))

(defn- do-summaries [this]
  (rx/observable*
    (fn [^Subscriber subscriber]
      (let [container (Container/of this)
            folder (.. container (produce PersistenceFolder) get)
            file (File. folder "conversation-summaries.tmp")
            admin (.produce container SneerAdmin)
            own-puk (.. admin privateKey publicKey)
            tuple-base (tuple-base-of admin)
            summaries-out (sliding-chan)
            summaries-out-mult (async/mult summaries-out)
            tap-summaries #(let [ch (sliding-chan)]
                             (async/tap summaries-out-mult ch)
                             ch)
            pretty-summaries-out (chan (sliding-buffer 1) (map to-foreign))
            lease (chan)]
        (link-chan-to-subscriber lease subscriber)
        (link-chan-to-subscriber summaries-out subscriber)
        (link-lease-to-chan lease pretty-summaries-out)
        (start-summarization-machine! (read-snapshot file) own-puk tuple-base summaries-out lease)
        (republish-latest-every (* 60 1000) (tap-summaries) pretty-summaries-out)
        (save-snapshots-to file (tap-summaries))
        (thread-chan-to-subscriber pretty-summaries-out subscriber "conversation summaries")))))

(defn reify-ConversationList []
  (reify ConversationList
    (summaries [this] (do-summaries this))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; OLD:

(defn- reify-notification [conversations title text subText]
  (reify Conversations$Notification
    (conversations [_] conversations)
    (title [_] title)
    (text [_] text)
    (subText [_] subText)))

(defn- unread-messages-label [count]
  (str count " unread message" (when-not (= 1 count) "s")))

(defn- notification-for-single [[^Conversation convo unread-messages]]
  (rx/map
   (fn [nick]
     (let [text (message-label (first unread-messages))
           subText (unread-messages-label (count unread-messages))]
       (reify-notification [convo] nick text subText)))
   (.. convo contact nickname observable)))

(defn- notification-for-many [unread-conversations]
  (let [conversations (mapv first unread-conversations)
        text ""
        unread-count (->> unread-conversations (map second) (map count) (reduce +))
        subText (unread-messages-label unread-count)]
    (rx/return
     (reify-notification conversations "New messages" text subText))))

(defn- notification-for-none []
  (rx/return
    (reify-notification [] nil nil nil)))

(defn- most-recent-message-timestamp [^Conversation conv]
  (let [^ObservedSubject subject (ObservedSubject/create 0)]
    (.subscribe ^Observable (.mostRecentMessageTimestamp conv) subject)
    (-> subject .observed .current (or 0))))

(defn reify-conversations [own-puk space contacts-state]
  (let [convos (atom {})
        reify-conversation (partial reify-conversation space own-puk)
        ignored-conversation (behavior-subject)
        contacts (get-contacts contacts-state)
        produce-convo (fn [contact] (do (println "FELIPETESTE: conversations/reify-conversations.produce-convo.contact->" contact) (produce! reify-conversation convos contact)))]

    (reify Conversations

      (all [_]
        (->> contacts
             (rx/map (partial mapv produce-convo))
             (rx/map (partial sort-by most-recent-message-timestamp #(compare %2 %1)))
             shared-latest))

      (ofType [_ _type]
        (rx/never))

      (withParty [this party]
        (some->> party
                 .publicKey
                 .current
                 (puk->contact contacts-state)
                 (.withContact this)))

      (withContact [_ contact]
        (println "FELIPETESTE: conversations/reify-conversations.withContact.contact->" contact)
        (produce-convo contact))

      (notifications [this]
        (->> [(.all this) ignored-conversation]

             ;; ([Conversation], Conversation)
             (combine-latest (fn [[all ignored]] (remove #(identical? % ignored) all)))

             ;; [Conversation]
             (rx/map (fn [conversations]
                       (->> conversations
                            (mapv (fn [^Conversation c]
                                    (->> (.unreadMessages c)
                                         (rx/map (partial vector c))))))))

             ;; [Observable (Conversation, [Message])]
             (switch-map
              (partial combine-latest
                       (partial filterv (comp not empty? second))))

             ;; [(Conversation, [unread Message])]
             (switch-map
              (fn [unread-pairs]
                (case (count unread-pairs)
                  0 (notification-for-none)
                  1 (notification-for-single (first unread-pairs))
                  (notification-for-many unread-pairs))))))

      (notificationsStartIgnoring [_ conversation] (.onNext ignored-conversation conversation))
      (notificationsStopIgnoring  [_]              (.onNext ignored-conversation nil))

      (findSessionById [_ id]
        (reify-session-by-id space own-puk id)))))
