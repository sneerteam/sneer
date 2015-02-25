(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [atom->observable subscribe-on-io]]
   [sneer.party :refer [party-puk]]
   [sneer.commons :refer [now produce!]])
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
      (tuple [_] tuple))))

(defn values-to-compare [^Message msg] [(-> msg .tuple (get "id"))])
(def message-comparator (fn [m1 m2] (compare (values-to-compare m1) (values-to-compare m2))))

(defn reify-conversation [^TupleSpace tuple-space ^Observable conversation-menu-items ^PublicKey own-puk ^Party party]
  (let [^PublicKey party-puk (party-puk party)
        messages (atom (sorted-set-by message-comparator))
        observable-messages (rx/map vec (atom->observable messages))
        message-filter (.. tuple-space filter (type "message"))
        unread-message-counter (atom 0)
        being-read (atom nil)
        msg-tuples-out (.. message-filter (author own-puk  ) (audience party-puk) tuples)
        msg-tuples-in  (.. message-filter (author party-puk) (audience own-puk  ) tuples)]

    (subscribe-on-io
      (rx/merge msg-tuples-out msg-tuples-in)
      (fn [tuple]
        (swap! messages conj (tuple->message own-puk tuple))))

    (subscribe-on-io
      msg-tuples-in
      (fn [_]
        (if-not @being-read (swap! unread-message-counter inc))))

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
        (atom->observable unread-message-counter))

      (setBeingRead [_ is-being-read]
        (reset! being-read is-being-read)
        (when is-being-read
          (reset! unread-message-counter 0))))))

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
