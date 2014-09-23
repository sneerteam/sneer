(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [subject* observe-for-computation atom->observable flatmapseq shared-latest]]
   [sneer.commons :refer [produce]])
  (:import
   [sneer Sneer PrivateKey PublicKey Party Contact Profile Conversation Message]
   [sneer.commons.exceptions FriendlyException]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
   [java.text SimpleDateFormat]
   [java.util Date]
   [rx.subjects Subject BehaviorSubject]))

(defprotocol PartyImpl
  (name-subject [this]))

(defn new-party [puk]
  (let [name (ObservedSubject/create (str "? PublicKey: " (-> puk .bytesAsString (subs 0 7)) "..."))]
    (reify
      Party
        (name [this] (.observable name))
        (publicKey [this]
          (.observed (ObservedSubject/create puk)))
        (toString [this]
          (str "#<Party " puk ">"))
      PartyImpl
        (name-subject [this] name))))

(defn party-puk [^Party party]
  (.. party publicKey current))

(defn produce-party [parties puk]
  (produce parties puk new-party))

(defn reify-profile [party ^TupleSpace tuple-space]

  (letfn [(payloads-of [type]
            (rx/map
             (fn [^Tuple tuple] (.payload tuple))
             (.. tuple-space
                 filter
                 (type type)
                 (author (party-puk party))
                 tuples)))

          (payload-subject [tuple-type]
            (let [latest (shared-latest (payloads-of tuple-type))
                  publish #(.. tuple-space
                               publisher
                               (type tuple-type)
                               (pub %))]
              (subject*
               latest
               (reify rx.Observer
                 (onNext [this value]
                   (publish value))))))]

    (let [^Subject preferred-nickname (payload-subject "profile/preferred-nickname")
          ^Subject own-name (payload-subject "profile/own-name")
          ^Subject selfie (payload-subject "profile/selfie")
          ^Subject city (payload-subject "profile/city")
          ^Subject country (payload-subject "profile/country")]

      (reify Profile
        (ownName [this]
          (.asObservable own-name))
        (setOwnName [this value]
          (rx/on-next own-name value))
        (selfie [this]
          (.asObservable selfie))
        (setSelfie [this value]
          (rx/on-next selfie value))
        (preferredNickname [this]
          (.asObservable preferred-nickname))
        (setPreferredNickname [this value]
          (rx/on-next preferred-nickname value))
        (city [this]
          (.asObservable city))
        (setCity [this value]
          (rx/on-next city value))
        (country [this]
          (.asObservable country))
        (setCountry [this value]
          (rx/on-next country value))))))

(defn produce-profile [tuple-space profiles party]
  (produce profiles party #(reify-profile % tuple-space)))

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
  (.getTime (java.util.Date.)))

(def simple-date-format (SimpleDateFormat. "HH:mm"))

(defn format-date [time] (.format simple-date-format (Date. time)))

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
