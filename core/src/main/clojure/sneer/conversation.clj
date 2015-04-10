(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [rx.lang.clojure.interop :as interop]
   [sneer.rx :refer [atom->observable subscribe-on-io latest shared-latest combine-latest switch-map]]
   [sneer.party :refer [party->puk]]
   [sneer.commons :refer [now produce!]]
   [sneer.contact :refer [get-contacts puk->contact]]
   [sneer.tuple.space :refer [payload]])
  (:import
    [sneer PublicKey Contact Conversation Message]
    [sneer.tuples Tuple TupleSpace]
    [java.text SimpleDateFormat]
    [rx Observable]))

(def simple-date-format (SimpleDateFormat. "HH:mm"))

(defn format-date [time] (.format ^SimpleDateFormat simple-date-format time))

(defn reify-message [own-puk ^Tuple tuple]
  (let [created (.timestamp tuple)
        type (.type tuple)
        jpeg-image ^bytes (.get tuple "jpeg-image")
        label (.get tuple "label")
        label (if label label (if jpeg-image "" type))
        own? (= own-puk (.author tuple))]

    (reify Message
      (isOwn [_] own?)
      (label [_] label)
      (jpegImage [_] jpeg-image)
      (timestampCreated [_] created)
      (timestampReceived [_] 0)
      (timeCreated [_] (format-date created))
      (tuple [_] tuple)
      Object
      (toString [_] label))))

(defn- message-id [^Message msg]
  (-> msg .tuple (get "id")))

(defn original-id [^Message message]
  (get (.tuple message) "original_id"))

(defn own? [^Message message]
  (.isOwn message))

(defn message-label [^Message message]
  (.label message))

(defn- message-timestamp [^Message message]
  (.timestampCreated message))

(defn- reverse-party-messages [messages]
  (->> messages reverse (remove own?)))

(defn- most-recent-message [^Observable observable-messages]
  (switch-map
   (fn [messages]
     (if-some [message (last messages)]
       (rx/return message)
       (rx/empty)))
   observable-messages))

(defn- unread-messages [messages last-read-id]
  (->> (reverse-party-messages messages)
       (take-while #(> (original-id %) last-read-id))
       vec))

(defn- latest-unread-messages
  [^Observable observable-messages ^Observable acks]
  (let [last-read-ids (rx/map payload acks)]
    (latest
     (Observable/combineLatest observable-messages
                               (rx/cons 0 last-read-ids)
                               (interop/fn [messages last-read-id]
                                 (unread-messages messages last-read-id))))))

(defn- message-ids [m1 m2]
  (compare (message-id m1)
           (message-id m2)))

(defn- messages [tuple-space own-puk party-puk]
  (let [filter (.. tuple-space filter (type "message"))
        tuples-out (.. filter (author own-puk  ) (audience party-puk) tuples)
        tuples-in  (.. filter (author party-puk) (audience own-puk  ) tuples)]
    (->> (rx/merge tuples-in tuples-out)
         (rx/map #(reify-message own-puk %))
         (rx/reductions conj (sorted-set-by message-ids))
         (rx/map vec))))

(defn reify-conversation
  [^TupleSpace tuple-space ^Observable conversation-menu-items ^PublicKey own-puk ^Contact contact]
  (let [get-party-puk #(-> contact .party .current party->puk)
        get-messages #(some->> (get-party-puk) (messages tuple-space own-puk))
        get-ack-pub #(when-let [party-puk (get-party-puk)]
                      (.. tuple-space publisher (type "message-read") (audience party-puk)))
        get-acks #(when-let [party-puk (get-party-puk)]
                   (.. tuple-space filter (type "message-read") (audience party-puk) (author own-puk) last tuples))
        get-unread-messages #(some-> (get-messages) (latest-unread-messages (get-acks)))
        get-most-recent-message #(some-> (get-messages) most-recent-message)]

    (reify
      Conversation
      (contact [_] contact)

      (canSendMessages [_] (rx/map some? (.. contact party observable)))

      (messages [_] (or (get-messages) (rx/never)))

      (unreadMessages [_] (or (get-unread-messages) (rx/never)))

      (sendMessage [_ label]
        (if-let [party-puk (get-party-puk)]
          (..
            tuple-space
            publisher
            (audience party-puk)
            (type "message")
            (field "message-type" "chat")
            (field "label" label)
            (pub))
          (throw (Exception. "Contact party doesn't exist yet."))))

      (mostRecentMessageContent [_]
        (or (some->> (get-most-recent-message) (rx/map message-label))
            (rx/never)))

      (mostRecentMessageTimestamp [_]
        (or (some->> (get-most-recent-message) (rx/map message-timestamp))
            (rx/never)))

      (menu [_]
        conversation-menu-items)

      (unreadMessageCount [_]
        (or (some->> (get-unread-messages) (rx/map (comp long count)))
            (rx/return 0)))

      (setRead [_ message]
        (assert (-> message own? not))
        (println "Publishing message read tuple.")          ;; Klaus: I suspect this might be happening too often, redundantly for already read messages.
        (if-let [ack-pub (get-ack-pub)]
          (.pub ack-pub (original-id message))
          (throw (Exception. "Contact party doesn't exist yet.")))))))