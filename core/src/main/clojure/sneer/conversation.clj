(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [rx.lang.clojure.interop :as interop]
   [sneer.rx :refer [atom->observable subscribe-on-io latest]]
   [sneer.party :refer [party-puk]]
   [sneer.commons :refer [now produce!]]
   [sneer.tuple.space :refer [payload]])
  (:import
    [sneer PublicKey Party Contact Conversation Message]
    [sneer.rx ObservedSubject]
    [sneer.tuples Tuple TupleSpace]
    [java.text SimpleDateFormat]
    [rx.subjects BehaviorSubject]
    [rx Observable]))

(def simple-date-format (SimpleDateFormat. "HH:mm"))

(defn format-date [time] (.format ^SimpleDateFormat simple-date-format time))

(defn tuple->message [own-puk ^Tuple tuple]
  ; Use MessageImpl.fromTuple instead of reimplementing it here.
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

(defn- latest-unread-message-count
  [^rx.Observable observable-messages ^rx.Observable last-read-filter]
  (let [last-read-ids (rx/map payload last-read-filter)]
    (latest
     (rx.Observable/combineLatest
      observable-messages
      (rx/cons 0 last-read-ids)
      (interop/fn [messages last-read-id]
        (->> messages
             reverse
             (remove own?)
             (take-while #(> (original-id %) last-read-id))
             count
             long))))))

(defn values-to-compare [^Message msg] [(-> msg .tuple (get "id"))])

(def message-comparator (fn [m1 m2] (compare (values-to-compare m1) (values-to-compare m2))))

(defn reify-conversation [^TupleSpace tuple-space ^Observable conversation-menu-items ^PublicKey own-puk ^Party party]
  (let [^PublicKey party-puk (party-puk party)
        messages (atom (sorted-set-by message-comparator))
        observable-messages (rx/map vec (atom->observable messages))
        message-filter (.. tuple-space filter (type "message"))
        msg-tuples-out (.. message-filter (author own-puk  ) (audience party-puk) tuples)
        msg-tuples-in  (.. message-filter (author party-puk) (audience own-puk  ) tuples)
        last-read-pub (.. tuple-space
                          publisher
                          (type "message-read")
                          (audience party-puk))
        last-read-filter (.. tuple-space
                             filter
                             last
                             (type "message-read")
                             (audience party-puk)
                             (author own-puk)
                             tuples)
        unread-message-count (latest-unread-message-count observable-messages last-read-filter)]

    (subscribe-on-io
      (rx/merge msg-tuples-out
                msg-tuples-in)
      (fn [tuple]
        (swap! messages conj (tuple->message own-puk tuple))))

    (reify
      Conversation
      (party [_] party)

      (messages [_]
        observable-messages)

      (sendMessage [_ label]
        (..
          tuple-space
          publisher
          (audience party-puk)
          (field "message-type" "chat")
          (type "message")
          (field "label" label)
          (pub)))

      (mostRecentMessageContent [_]
        (.observed (ObservedSubject/create "")))

      (mostRecentMessageTimestamp [_]
        (.observed (ObservedSubject/create (now))))

      (menu [_] conversation-menu-items)

      (unreadMessageCount [_]
        unread-message-count)

      (setRead [_ message]
        (.pub last-read-pub (original-id message))))))

(defn produce-conversation [tuple-space conversation-menu-items own-puk party convos]
  (produce! #(reify-conversation tuple-space (.asObservable ^BehaviorSubject conversation-menu-items) own-puk %) convos party))

(defn create-conversations-state [own-puk tuple-space contacts conversation-menu-items]
  {:own-puk own-puk
   :tuple-space tuple-space
   :contacts contacts
   :conversation-menu-items conversation-menu-items})

(defn conversations [{:keys [own-puk tuple-space contacts conversation-menu-items]} convos]
  (->>
    contacts
    (rx/map
      (partial map (fn [^Contact c] (produce-conversation tuple-space conversation-menu-items own-puk (.party c) convos))))))

(defn produce-conversation-with [{:keys [own-puk tuple-space conversation-menu-items]} party convos]
  (produce-conversation tuple-space conversation-menu-items own-puk party convos))
