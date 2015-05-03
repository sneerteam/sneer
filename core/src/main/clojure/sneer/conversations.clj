(ns sneer.conversations
  (:require
    [clojure.core.async :refer [go chan close! <! >! sliding-buffer]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [go-trace link-chan-to-subscriber thread-chan-to-subscriber]]
    [sneer.commons :refer [now produce!]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.conversation :refer :all]
    [sneer.flux.macros :refer :all]
    [sneer.rx :refer [atom->observable subscribe-on-io latest shared-latest combine-latest switch-map behavior-subject]]
    [sneer.party :refer [party->puk]]
    [sneer.tuple.persistent-tuple-base :as ptb]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple.space :refer [payload]]
    [sneer.tuple-base-provider :refer :all])
  (:import
    [rx Subscriber Observable]
    [sneer Conversations Conversation Conversations$Notification]
    [sneer.admin SneerAdmin]
    [sneer.commons Container]
    [sneer.conversations ConversationList ConversationList$Summary ConversationList]
    [sneer.rx ObservedSubject]))

(defn create-summarization-state []
  {})

(defn- contact-puk [tuple]
  (tuple "party"))

(defn update-contact [contact state]
  (let [contact-id (contact-puk contact)
        contact-name (contact "payload")
        timestamp (contact "timestamp")]
    (assoc state contact-id {:name contact-name :summary "" :timestamp timestamp :unread 0})))

(defn update-message [own-puk message state]
  (let [author (message "author")
        contact-puk (if (= author own-puk) (message "audience") author)]
    (-> state (update contact-puk assoc
                      :summary (or (message "label") "")
                :timestamp (message "timestamp"))
              (update-in [contact-puk :unread] inc))))

(defn flip [f]
  (fn [x y] (f y x)))

(defn summarize [state]
  (->> state
       vals
       (sort-by :timestamp (flip compare))))

(defn summarization-state-from-contacts [contacts]
  (reduce
   (flip update-contact)
   (create-summarization-state)
   contacts))

(defn start-summarization-machine [own-puk tuple-base summaries-out lease]
  (let [tuples (chan)]

    ; link machine lifetime to lease
    (go (println "SUMMARIZATION STARTED")
        (<! lease)
        (close! tuples)
        (println "SUMMARIZATION STOPPED"))

    (go-trace

     ; create initial summarization state based on all contacts
     (let [contact-query {"type" "contact" "author" own-puk "audience" own-puk}
           existing-contacts (<! (ptb/query-all tuple-base contact-query))
           state (summarization-state-from-contacts existing-contacts)

           last-contact-id (-> existing-contacts last (get "id"))
           existing-contacts nil

           ; convenience functions
           query-tuples (fn [criteria]
                          (query-tuples tuple-base criteria tuples lease))

           query-messages (fn [contact-puk]
                            (let [message-query {"type" "message" ptb/last-by-id true}]
                              (query-tuples (assoc message-query "audience" own-puk "author" contact-puk))
                              ; there's a subtle issue here, since we are emitting two separate queries onto the same
                              ; channel there's no guarantee the tuples will be ordered by id
                              ; a good solution would be to extend query-tuples criteria to support :or clauses
                              (query-tuples (assoc message-query "author" own-puk "audience" contact-puk))))]

       ; keep an eye on messages from existing contacts
       (doseq [contact-puk (keys state)]
         (query-messages contact-puk))

       ; query new contact tuples
       (query-tuples (assoc contact-query ptb/after-id last-contact-id))

       ; update conversation summaries
       (loop [state state]
         (>! summaries-out (summarize state))
         (when-some [tuple (<! tuples)]
           (case (tuple "type")
             "contact"
             (if-some [puk (contact-puk tuple)]
               (do
                 ; keep an eye on messages from new contact
                 (query-messages puk)
                 ; update
                 (recur (update-contact tuple state)))
               ; ignore contacts without a party for now
               (recur state))

             "message"
             (recur
              (update-message own-puk tuple state)))))))))


; Java interface

(defn to-foreign-summary [{:keys [name summary timestamp unread]}]
  (println "TODO: SUMMARY ID")
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
        (start-summarization-machine own-puk tuple-base summaries-out lease)
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

(defn reify-conversations [own-puk tuple-space contacts-state]
  (let [convos (atom {})
        reify-conversation (partial reify-conversation tuple-space own-puk)
        ignored-conversation (behavior-subject)
        contacts (get-contacts contacts-state)
        produce-convo (fn [contact] (produce! reify-conversation convos contact))]

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
      (notificationsStopIgnoring  [_]              (.onNext ignored-conversation nil)))))
