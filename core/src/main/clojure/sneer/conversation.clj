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
    [sneer PublicKey Contact Conversation Message Session]
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
                             (take-while #(and (> (message-id %) last-read-id)
                                               (recent? %)))  ;TODO Remove this "recent" hack and fix redundant notification generation.
                             vec)))

(defn- unread-messages [^Observable messages ^Observable last-read]
  (let [last-read-id (->> last-read (map-some payload 0) (rx/cons 0))]
    (latest
      (Observable/combineLatest messages last-read-id unread))))

(defn- message-ids [m1 m2]
  (compare (message-id m1)
           (message-id m2)))

(defn- session-ids [s1 s2]
  (compare (.id s1)
           (.id s2)))

(defn- messages [tuple-space own-puk party-puk]
  (let [filter (.. tuple-space filter (type "message"))
        tuples-out (.. filter (author own-puk  ) (audience party-puk) tuples)
        tuples-in  (.. filter (author party-puk) (audience own-puk  ) tuples)]
    (->> (rx/merge tuples-in tuples-out)
         (rx/map #(reify-message own-puk %))
         (rx/reductions conj (sorted-set-by message-ids))
         (rx/map vec)
         (rx/cons [])
         shared-latest)))

(defn reify-session [space own-puk contact-puk tuple]
  (let [id (get tuple "original_id")
        author (get tuple "author")
        publisher (.. space publisher (audience contact-puk) (type "message") (field "ref" id) (field "session_author" author))
        filter (.. space filter (type "message") (field "ref" id) (field "session_author" author))]
    (reify Session
      (id [_] id)
      (messages [_]
        (rx/merge (.. filter (audience contact-puk) (author own-puk)     tuples)
                  (.. filter (audience own-puk)     (author contact-puk) tuples)))
      (send [_ payload]
        (.pub publisher payload)))))

(defn- sessions [tuple-space own-puk party-puk]
  (let [filter (.. tuple-space filter (type "session"))
        tuples-out (.. filter (author own-puk  ) (audience party-puk) tuples)
        tuples-in  (.. filter (author party-puk) (audience own-puk  ) tuples)]
    (->> (rx/merge tuples-in tuples-out)
         (rx/map #(reify-session tuple-space own-puk party-puk %))
         (rx/reductions conj (sorted-set-by session-ids))
         (rx/map vec)
         (rx/cons [])
         shared-latest)))

(defn- start-session [space own-puk contact-puk #_session-type]
  (let [tuple-obs (.. space publisher (audience contact-puk) (type "session") (field "session-type" #_session-type "TODO") pub)]
    (rx/map #(reify-session space own-puk contact-puk %)
            tuple-obs)))

(defn reify-conversation
  [^TupleSpace space ^PublicKey own-puk ^Contact contact]
  (let [party (.. contact party observable)
        puk (switch-map-some #(.. % publicKey observable) party)

        sessions (switch-map-some #(sessions space own-puk %) [] puk)
        messages (switch-map-some #(messages space own-puk %) [] puk)

        last-read-filter #(.. space filter (type "message-read") (audience %) (author own-puk) last tuples)
        last-read (switch-map-some last-read-filter puk)

        most-recent-message (rx/map last messages)

        contact-puk #(doto
                      (some-> contact .party .current .publicKey .current)
                      (assert "Contact does not have a known public key."))

        sender #(.. space publisher (audience (contact-puk)))
        message-sender      #(.. (sender) (type "message"     ) (field "message-type" "chat"))
        message-read-sender #(.. (sender) (type "message-read"))]

    (reify
      Conversation
      (contact [_] contact)

      (canSendMessages [_] (rx/map some? party))
      (sendMessage [_ label] (.. (message-sender) (field "label" label) pub))

      (sessions [_] sessions)
      (items [_] messages)
      (mostRecentMessageContent   [_] (map-some message-label     most-recent-message))
      (mostRecentMessageTimestamp [_] (map-some message-timestamp most-recent-message))

      (unreadMessages [_] (unread-messages messages last-read))
      (unreadMessageCount [this] (rx/map (comp long count) (.unreadMessages this)))
      (setRead [_ message] (.pub (message-read-sender) (message-id message)))

      (startSession [_] (start-session space own-puk (contact-puk))))))
