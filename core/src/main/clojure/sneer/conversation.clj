(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [rx.lang.clojure.interop :as interop]
   [sneer.rx :refer [atom->observable subscribe-on-io latest shared-latest combine-latest switch-map switch-map-some map-some]]
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

(def unread (interop/fn [messages last-read-id]
                        (->> (reverse-party-messages messages)
                             (take-while #(> (original-id %) last-read-id))
                             vec)))

(defn- unread-messages  [^Observable messages ^Observable last-read]
  (let [last-read-id (->> last-read (map-some payload 0) (rx/cons 0))]
    (latest
      (Observable/combineLatest messages last-read-id unread))))

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
  [^TupleSpace tuple-space ^PublicKey own-puk ^Contact contact]
  (let [party (.. contact party observable)
        puk (switch-map-some #(.. % publicKey observable) party)

        messages (switch-map-some #(messages tuple-space own-puk %) [] puk)

        last-read-filter #(.. tuple-space filter (type "message-read") (audience %) (author own-puk) last tuples)
        last-read (switch-map-some last-read-filter puk)

        most-recent-message (rx/map last messages)

        current-puk #(doto
                      (some-> contact .party .current .publicKey .current)
                      (assert "Contact does not have a known public key."))

        sender #(.. tuple-space publisher (audience (current-puk)))
        message-sender      #(.. (sender) (type "message"     ) (field "message-type" "chat"))
        message-read-sender #(.. (sender) (type "message-read"))]

    (reify
      Conversation
      (contact [_] contact)

      (canSendMessages [_] (rx/map some? party))
      (sendMessage [_ label] (.. (message-sender) (field "label" label) pub))

      (messages [_] messages)
      (mostRecentMessageContent   [_] (map-some message-label     most-recent-message))
      (mostRecentMessageTimestamp [_] (map-some message-timestamp most-recent-message))

      (unreadMessages [_] (unread-messages messages last-read))
      (unreadMessageCount [this] (rx/map (comp long count) (.unreadMessages this)))
      (setRead [_ message] (.pub (message-read-sender) (original-id message))))))