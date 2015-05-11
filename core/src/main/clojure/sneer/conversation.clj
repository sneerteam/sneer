(ns sneer.conversation
  (:require
    [clojure.core.async :refer [chan <! >!]]
    [rx.lang.clojure.core :as rx]
    [rx.lang.clojure.interop :as interop]
    [sneer.async :refer [thread-chan-to-subscriber link-chan-to-subscriber go-trace]]
    [sneer.commons :refer [now produce! while-let]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.protocols :refer :all]
    [sneer.rx :refer [atom->observable subscribe-on-io latest shared-latest combine-latest switch-map switch-map-some map-some]]
    [sneer.party :refer [party->puk]]
    [sneer.contact :refer [get-contacts puk->contact]]
    [sneer.tuple.space :refer [payload reify-tuple]]
    [sneer.tuple.persistent-tuple-base :as tb])
  (:import
    [sneer PublicKey Contact Conversation Message Session Session$MessageOrUpToDate]
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
      (id [_] (get tuple "id"))
      (isOwn [_] own?)
      (label [_] label)
      (jpegImage [_] jpeg-image)
      (timestampCreated [_] created)
      (timestampReceived [_] 0)
      (timeCreated [_] (format-date created))
      (tuple [_] tuple)
      (payload [_] (.payload tuple))
      Object
      (toString [_] label))))

(defn own? [^Message message]
  (.isOwn message))

(defn message-label [^Message message]
  (.label message))

(defn- message-timestamp [^Message message]
  (.timestampCreated message))

(defn- reverse-party-messages [messages]
  (->> messages reverse (remove own?)))

(def ten-minutes (* 1000 60 10))
(defn- age [message] (- (System/currentTimeMillis) (.timestampCreated message)))
(defn- recent? [message] (< (age message) ten-minutes))

(def unread (interop/fn [messages last-read-id]
                        (->> (reverse-party-messages messages)
                             (take-while #(and (> (.id %) last-read-id)
                                               (recent? %)))  ;TODO Remove this "recent" hack and fix redundant notification generation.
                             vec)))

(defn- unread-messages [^Observable messages ^Observable last-read]
  (let [last-read-id (->> last-read (map-some payload 0) (rx/cons 0))]
    (latest
      (Observable/combineLatest messages last-read-id unread))))

(defn- item-ids [item1 item2]
  (compare (.id item1)
           (.id item2)))

(defn reify-session [space own-puk contact-puk tuple]
  (let [original-id (get tuple "original_id")
        author (get tuple "author")
        publisher (.. space publisher (type "session-message") (field "session-id" original-id) (field "session-author" author) (audience contact-puk))
        created (.timestamp tuple)]
    
    (reify Session
      (id [_] (get tuple "id"))
      (isOwn [_] (= own-puk author))
      (label [this] (.type this))
      (jpegImage [_] ^bytes (.get tuple "jpeg-image"))
      (timestampCreated [_] created)
      (timestampReceived [_] 0)
      (timeCreated [_] (format-date created))
      (tuple [_] tuple)
      (type [_] (get tuple "session-type"))
      (messages [_]
        (rx/observable*
          (fn [^rx.Subscriber s]
            (let [tb (tuple-base-of space)
                  criteria {"type" "session-message"
                            "session-id" original-id
                            "session-author" author}
                  reify-message-or-up-to-date #(if (= ::up-to-date %)
                                                Session/UP_TO_DATE
                                                (Session$MessageOrUpToDate. (reify-message own-puk (reify-tuple %))))
                  messages (chan 1 (map reify-message-or-up-to-date))
                  lease (chan)]
              (tb/store-sub tb own-puk (assoc criteria "author" contact-puk))
              (thread-chan-to-subscriber messages s "Session messages")
              (link-chan-to-subscriber lease s)
              (go-trace
                (let [tuples (chan)
                      last-id (atom -1)]
                  (query-tuples tb criteria tuples)
                  (while-let [t (<! tuples)]
                             (>! messages t)
                             (reset! last-id (get "id" t)))
                  (>! messages ::up-to-date)
                  (query-tuples tb (assoc criteria tb/after-id @last-id) messages lease)))))))
      (send [_ payload]
        (.pub publisher payload)))))

(defn other-party [tuple own-puk]
  (if (= (.author tuple) own-puk)
    (.audience tuple)
    (.author tuple)))

(defn reify-session-by-id [space own-puk id]
  (let [filter (.. space filter (field "id" id))
        tuple  (.. filter localTuples toBlocking first)]
    (reify-session space own-puk (other-party tuple own-puk) tuple)))

(defn reify-item [space own-puk party-puk tuple]
  (case (.type tuple)
    "message" (reify-message own-puk tuple)
    "session" (reify-session space own-puk party-puk tuple)))

(defn- items [space own-puk party-puk type]
  (let [filter (.. space filter (type type))
        tuples-out (.. filter (author own-puk  ) (audience party-puk) tuples)
        tuples-in  (.. filter (author party-puk) (audience own-puk  ) tuples)]
    (->> (rx/merge tuples-in tuples-out)
         (rx/map #(reify-item space own-puk party-puk %)))))

(defn- lists-sorted-by-id [items]
  (->> items
       (rx/reductions conj (sorted-set-by item-ids))
       (rx/map vec)
       (rx/cons [])
       shared-latest))

(defn- start-session [space own-puk contact-puk session-type]
  (let [tuple-obs (.. space publisher (audience contact-puk) (type "session") (field "session-type" session-type) pub)]
    (rx/map #(reify-session space own-puk contact-puk %)
            tuple-obs)))

(defn reify-conversation
  [^TupleSpace space ^PublicKey own-puk ^Contact contact]
  (let [party (.. contact party observable)
        contact-puks (switch-map-some #(.. % publicKey observable) party)

        messages (fn [contact-puk] (items space own-puk contact-puk "message"))
        sessions (fn [contact-puk] (items space own-puk contact-puk "session"))
        contact->lists (fn [fn] (switch-map-some #(lists-sorted-by-id (fn %)) [] contact-puks))
        message-lists (contact->lists messages)
        session-lists (contact->lists sessions)
        items-lists   (contact->lists #(rx/merge (messages %) (sessions %)))

        last-read-filter #(.. space filter (type "message-read") (audience %) (author own-puk) last tuples)
        last-read (switch-map-some last-read-filter contact-puks)

        most-recent-message (rx/map last message-lists)

        contact-puk #(doto
                      (some-> contact .party .current .publicKey .current)
                      (assert "Contact does not have a known public key."))

        sender #(.. space publisher (audience (contact-puk)))
        message-sender #(.. (sender) (type "message") (field "message-type" "chat"))
        message-read-sender #(.. (sender) (type "message-read"))]

    (reify
      Conversation
      (contact [_] contact)

      (canSendMessages [_] (rx/map some? party))
      (sendMessage [_ label] (.. (message-sender) (field "label" label) pub))

      (sessions [_] session-lists)
      (items [_] items-lists)
      (mostRecentMessageContent [_] (map-some message-label most-recent-message))
      (mostRecentMessageTimestamp [_] (map-some message-timestamp most-recent-message))

      (unreadMessages [_] (unread-messages message-lists last-read))
      (unreadMessageCount [this] (rx/map (comp long count) (.unreadMessages this)))
      (setRead [_ message] (.pub (message-read-sender) (.id message)))

      (startSession [_ type] (start-session space own-puk (contact-puk) type)))))
