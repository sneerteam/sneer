(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [atom->observable subscribe-on-io]]
   [sneer.party :refer [party-puk]]
   [sneer.commons :refer [now]])
  (:import
   [sneer PublicKey Party Contact Conversation Message]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace]
   [java.text SimpleDateFormat]
   [rx.subjects BehaviorSubject]))

(def simple-date-format (SimpleDateFormat. "HH:mm"))

(defn format-date [time] (.format ^SimpleDateFormat simple-date-format time))

(defn tuple->message [own-puk ^Tuple tuple]
  ; Use MessageImpl.fromTuple instead of reimplementing it here.
  (let [created (.timestampCreated tuple)
        type (.type tuple)
        jpeg-image ^bytes (.get tuple "jpeg-image")
        text (.get tuple "text")
        label (if text text (if jpeg-image "" type))
        own? (= own-puk (.author tuple))]
    
    (reify Message
      (isOwn [this] own?)
      (label [this] label)
      (jpegImage [this] jpeg-image)
      (timestampCreated [this] created)
      (timestampReceived [this] 0)
      (timeCreated [this] (format-date created))
      (tuple [this] tuple))))

(defn values-to-compare [^Message msg] [(.timestampCreated msg) (.label msg)])
(def message-comparator (fn [m1 m2] (compare (values-to-compare m1) (values-to-compare m2))))

(defn reify-conversation [^TupleSpace tuple-space ^rx.Observable conversation-menu-items ^PublicKey own-puk ^Party party]
  (let [^PublicKey party-puk (party-puk party)
        messages (atom (sorted-set-by message-comparator))
        observable-messages (rx/map vec (atom->observable messages))
        message-filter (.. tuple-space filter (type "message"))
        unread-message-counter (atom 0)
        being-read (atom nil)
        msg-tuples-out (.. message-filter (author own-puk) (audience party-puk) tuples)
        msg-tuples-in  (.. message-filter (author party-puk) (audience own-puk) tuples)]
    
    (subscribe-on-io
      (rx/merge msg-tuples-out msg-tuples-in)
      (fn [tuple]
        (swap! messages conj (tuple->message own-puk tuple))))
    
    (subscribe-on-io
      msg-tuples-in
      (fn [_]
        (if-not @being-read (swap! unread-message-counter 0))))
    
    (reify
      Conversation
      (party [this] party)
      
      (messages [this]
        observable-messages)
      
      (sendText [this text]
        (..
          tuple-space
          publisher
          (audience party-puk)
          (field "message-type" "chat")
          (type "message")
          (field "text" text)
          (pub)))
      
      (mostRecentMessageContent [this]
        (.observed (ObservedSubject/create "hello")))
      
      (mostRecentMessageTimestamp [this]
        (.observed (ObservedSubject/create (now))))
    
      (menu [this] conversation-menu-items)
    
      (unreadMessageCount [this]
        (atom->observable unread-message-counter))
    
      (setBeingRead [this is-being-read]
        (swap! being-read (fn [_] is-being-read))
        (if @being-read (swap! unread-message-counter (fn [_] 0)))))))

(defn produce-conversation [tuple-space conversation-menu-items own-puk party]
  (reify-conversation tuple-space (.asObservable ^BehaviorSubject conversation-menu-items) own-puk party))

(defn create-conversations-state [own-puk tuple-space contacts conversation-menu-items]
  {:own-puk own-puk
   :tuple-space tuple-space
   :contacts contacts
   :conversation-menu-items conversation-menu-items})

(defn conversations [{:keys [own-puk tuple-space contacts conversation-menu-items]}]
  (->>
    contacts
    (rx/map
      (partial map (fn [^Contact c] (produce-conversation tuple-space conversation-menu-items own-puk (.party c)))))))

(defn produce-conversation-with [{:keys [own-puk tuple-space contacts conversation-menu-items]} party]
  (produce-conversation tuple-space conversation-menu-items own-puk party))
