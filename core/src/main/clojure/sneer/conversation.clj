(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [rx.lang.clojure.interop :as interop]
   [sneer.rx :refer [atom->observable subscribe-on-io latest shared-latest combine-latest switch-map]]
   [sneer.party :refer [party-puk]]
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

(defn values-to-compare [^Message msg] [(-> msg .tuple (get "id"))])

(def message-comparator (fn [m1 m2] (compare (values-to-compare m1) (values-to-compare m2))))

(defn- messages [tuple-space own-puk party-puk]
  (let [filter (.. tuple-space filter (type "message"))
        tuples-out (.. filter (author own-puk  ) (audience party-puk) tuples)
        tuples-in  (.. filter (author party-puk) (audience own-puk  ) tuples)]
    (->> (rx/merge tuples-in tuples-out)
         (rx/map #(reify-message own-puk %))
         (rx/reductions conj (sorted-set-by message-comparator))
         (rx/map vec))))

(defn reify-conversation
  [^TupleSpace tuple-space ^Observable conversation-menu-items ^PublicKey own-puk ^Contact contact]
  (let [^PublicKey party-puk (-> contact .party .current party-puk)
        messages (messages tuple-space own-puk party-puk)
        ack-pub  (.. tuple-space publisher (type "message-read") (audience party-puk))
        acks     (.. tuple-space filter    (type "message-read") (audience party-puk) (author own-puk) last tuples)
        unread-messages (latest-unread-messages messages acks)
        most-recent-message (most-recent-message messages)]

    (reify
      Conversation
      (contact [_] contact)

      (canSendMessages [_] (rx/map some? (.. contact party observable)))

      (messages [_]
        #_(let [puks (switch-map (fn [party]
                                 (or (some-> party .publicKey)
                                     (rx/never)))
                               (.. contact party observable))]
          (rx/map (fn [puk]) puks))
        messages)

      (unreadMessages [_]
        unread-messages)

      (sendMessage [_ label]
        (..
          tuple-space
          publisher
          (audience party-puk)
          (type "message")
          (field "message-type" "chat")
          (field "label" label)
          (pub)))

      (mostRecentMessageContent [_]
        (rx/map message-label most-recent-message))

      (mostRecentMessageTimestamp [_]
        (rx/map message-timestamp most-recent-message))

      (menu [_]
        conversation-menu-items)

      (unreadMessageCount [_]
        (rx/map (comp long count) unread-messages))

      (setRead [_ message]
        (assert (-> message own? not))
        (println "Publishing message read tuple.")          ;; Klaus: I suspect this might be happening too often, redundantly for already read messages.
        (.pub ack-pub (original-id message))))))