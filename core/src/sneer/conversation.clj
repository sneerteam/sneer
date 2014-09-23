(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [atom->observable]]
   [sneer.party :refer [name-subject produce-party party-puk]])
  (:import
   [sneer PublicKey Party Contact Conversation Message]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace]
   [java.text SimpleDateFormat]))

(defn reify-contact [nickname party]
  (let [nick-subject (ObservedSubject/create nickname)]
    (.subscribe (.observable nick-subject) (name-subject party))
    (reify Contact
      (party [this] party)
      (nickname [this]
        (.observed nick-subject))
      (setNickname [this new-nick]
        (rx/on-next nick-subject new-nick))
      (toString [this]
        (str "#<Contact " (.current nick-subject) ">")))))

(defn tuple->contact [^Tuple tuple parties]
  (reify-contact (.payload tuple)
                 (produce-party parties (.get tuple "party"))))

(defn restore-contact-list [^TupleSpace tuple-space own-puk parties]
  (->>
   (.. tuple-space
       filter
       (author own-puk)
       (type "contact")
       localTuples
       toBlocking
       toIterable)
   (mapcat (fn [^Tuple tuple] [(.get tuple "party") (tuple->contact tuple parties)]))
   (apply hash-map)))

(defn current-nickname [^Contact contact]
  (.. contact nickname current))

(defn now []
  (System/currentTimeMillis))

(def simple-date-format (SimpleDateFormat. "HH:mm"))

(defn format-date [time] (.format simple-date-format (now)))

(defn tuple->message [own-puk ^Tuple tuple]
  (let [created (now)
        received (now)
        type (.type tuple)
        label (.get tuple "label")
        content (if (= type "message") (.payload tuple) (if label label type))
        own? (= own-puk (.author tuple))]
    
    (reify Message
      (isOwn [this] own?)
      (content [this] content)
      (timestampCreated [this] created)
      (timestampReceived [this] received)
      (timeCreated [this] (format-date created))
      (tuple [this] tuple))))

(defn reify-conversation [^TupleSpace tuple-space ^rx.Observable conversation-menu-items ^PublicKey own-puk ^Party party]
  (let [^PublicKey party-puk (party-puk party)
        messages (atom [])
        observable-messages (atom->observable messages)
        message-filter (.. tuple-space filter #_(type "message"))]
    
    (rx/subscribe
      (rx/subscribe-on
        (rx.schedulers.Schedulers/io)
        (rx/merge
          (.. message-filter (author own-puk) (audience party-puk) tuples)
          (.. message-filter (author party-puk) (audience own-puk) tuples)))
      #(swap! messages conj (tuple->message own-puk %)))
    
    (reify
      Conversation      
      (party [this] party)
      
      (messages [this]
        observable-messages)
      
      (sendMessage [this content]
        (..
          tuple-space
          publisher
          (audience party-puk)
          (type "message")
          (pub content)))
      
      (mostRecentMessageContent [this]
        (.observed (ObservedSubject/create "hello")))
      
      (mostRecentMessageTimestamp [this]
        (.observed (ObservedSubject/create (now))))
    
      (menu [this] conversation-menu-items)
    
      (unreadMessageCount [this] (rx.Observable/just 1))
    
      (unreadMessageCountReset [this]))))
