(ns sneer.conversations
  (:require
    [clojure.core.async :refer [go chan close! <! >! sliding-buffer]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [go-trace link-chan-to-subscriber thread-chan-to-subscriber]]
    [sneer.commons :refer [now produce! descending update-java-map]]
    [sneer.clojure.core :refer [nvl]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.conversation :refer :all]
    [sneer.rx :refer [atom->observable subscribe-on-io latest shared-latest combine-latest switch-map behavior-subject]]
    [sneer.party :refer [party->puk]]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple.space :refer [payload]]
    [sneer.tuple-base-provider :refer :all])
  (:import
    [rx Subscriber Observable]
    [sneer Conversations Conversation Conversations$Notification]
    [sneer.admin SneerAdmin]
    [sneer.commons Container]
    [sneer.conversations ConversationList ConversationList$Summary ConversationList]
    [sneer.rx ObservedSubject]
    (java.util HashMap)))

(defn- contact-puk [tuple]
  (tuple "party"))

(defn- handle-contact! [own-puk tuple state]
  (when (= (tuple "author") own-puk)
    (when-let [contact-puk (contact-puk tuple)]
      (update-java-map state contact-puk #(assoc %
                                           :name      (tuple "payload")
                                           :timestamp (tuple "timestamp"))))))

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
        (update-with-received (message "id") label old-summary))
      (assoc
        :preview (or label "")
        :timestamp timestamp))))

(defn- handle-message! [own-puk message state]
  (let [author (message "author")
        own? (= author own-puk)
        contact-puk (if own? (message "audience") author)]
    (update-java-map state contact-puk #(update-summary own? message %))))

(defn update-with-read [old-summary tuple]
  (let [msg-id (tuple "payload")]
    (cond-> old-summary
      (= msg-id (old-summary :last-received)) (assoc :unread ""))))

(defn- handle-msg-read! [own-puk tuple state]
  (when (= (tuple "author") own-puk)
    (let [contact-puk (tuple "audience")]
      (update-java-map state contact-puk #(update-with-read % tuple)))))

(defn- summarize [state]
  (->> state
       .values
       (filter :name)
       (sort-by :timestamp descending)
       vec))

(defn start-summarization-machine! [own-puk tuple-base summaries-out lease]
  (let [tuples (chan)
        query-tuples (fn [criteria] (query-tuples tuple-base criteria tuples lease))
        state (HashMap.)]
    (query-tuples {})

    (go
      ; link machine lifetime to lease
      (<! lease)
      (close! tuples))

    (go-trace
      (loop []
        (>! summaries-out (summarize state))
        (when-some [tuple (<! tuples)]
          (case (tuple "type")
            "contact"      (handle-contact!  own-puk tuple state)
            "message"      (handle-message!  own-puk tuple state)
            "message-read" (handle-msg-read! own-puk tuple state))
          (recur))))))


; Java interface

(defn to-foreign-summary [{:keys [name summary timestamp unread]}]
  (println "TODO: CONVERSATION ID")
  (ConversationList$Summary. name summary (str timestamp) (str unread) -4242))

(defn do-summaries [this]
  (rx/observable*
    (fn [^Subscriber subscriber]
      (let [admin (.produce (Container/of this) SneerAdmin)
            own-puk (.. admin privateKey publicKey)
            tuple-base (tuple-base-of admin)
            to-foreign (map (fn [summaries] (mapv to-foreign-summary summaries)))
            summaries-out (chan (sliding-buffer 1) to-foreign)
            lease (chan)]
        (link-chan-to-subscriber lease subscriber)
        (link-chan-to-subscriber summaries-out subscriber)
        (start-summarization-machine! own-puk tuple-base summaries-out lease)
        (thread-chan-to-subscriber summaries-out subscriber "conversation summaries")))))

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
