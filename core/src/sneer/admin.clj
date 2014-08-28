(ns sneer.admin
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [subject* observe-for-computation flatmapseq]]
   [sneer.core :as core :refer [connect dispose restarted]]
   [sneer.persistent-tuple-base :as persistence]
   [clojure.java.io :as io])
  (:import
   [sneer.admin SneerAdmin]
   [sneer.commons.exceptions FriendlyException]
   [sneer Sneer PrivateKey Party Contact Profile Conversation]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
   [rx.schedulers TestScheduler]
   [rx.subjects BehaviorSubject ReplaySubject PublishSubject]))

(defn behavior-subject [& [initial-value]]
  (BehaviorSubject/create initial-value))

(defn replay-last-subject []
  (ReplaySubject/createWithSize 1))

(defprotocol PartyImpl
  (party-name-subject [this]))

(defn new-party [puk]
  (let [name (replay-last-subject)]
	  (reify
      Party
	    (name [this] name)
	    (publicKey [this]
	      (.observed (ObservedSubject/create puk)))
      PartyImpl
      (party-name-subject [this] name))))

(defn party-puk [party]
  (.. party publicKey current))

(defn produce-party [parties puk]
  (->
   (swap! parties update-in [puk] #(if (nil? %) (new-party puk) %))
   (get puk)))

(defn reify-profile [party tuple-space]

  (letfn [(payloads-of [type]
            (rx/map
             #(.payload %)
             (.. tuple-space
                 filter
                 (type type)
                 (author (party-puk party))
                 tuples)))

          (payload-subject [tuple-type]
            (let [subject (replay-last-subject)
                  publish #(.. tuple-space
                               publisher
                               (type tuple-type)
                               (pub %))]
              (rx/subscribe (payloads-of tuple-type)
                            (partial rx/on-next subject))
              (subject*
               (.asObservable subject)
               (reify rx.Observer
                 (onNext [this value]
                   (publish value))))))]

    (let [preferred-nickname (payload-subject "profile/preferred-nickname")
          own-name (payload-subject "profile/own-name")
          selfie (payload-subject "profile/selfie")
          city (payload-subject "profile/city")
          country (payload-subject "profile/country")]

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
  (let [puk (party-puk party)]
	  (->
	   (swap! profiles update-in [puk] #(if (nil? %) (reify-profile party tuple-space) %))
	   (get puk))))


(defn reify-contact [nickname party]
  (let [party-name-subject (party-name-subject party)
        nickname-subject (ObservedSubject/createWithSubject party-name-subject)]
    (rx/on-next party-name-subject nickname)
    (reify Contact
      (party [this] party)
      (nickname [this]
        (.observed nickname-subject))
      (setNickname [this new-nickname]
        (.set nickname-subject new-nickname))
      (toString [this]
        (str "#<Contact " (.. nickname-subject observed current) ">")))))

(defn tuple->contact [tuple parties]
  (reify-contact (.payload tuple)
                 (produce-party parties (.get tuple "party"))))

(defn restore-contact-list [tuple-space own-puk parties]
  (->>
   (.. tuple-space
       filter
       (author own-puk)
       (type "sneer/contact")
       localTuples
       toBlocking
       toIterable)
   (mapcat (fn [tuple] [(.get tuple "party") (tuple->contact tuple parties)]))
   (apply hash-map)))

(defn nickname [contact]
  (.. contact nickname current))

(defn reify-conversation [party]
  
  (reify
    Conversation
    (party [this] party)
    
    (mostRecentMessageContent [this]
      (.observed (ObservedSubject/create "hello")))
    
    (mostRecentMessageTimestamp [this]
      (.observed (ObservedSubject/create (.getTime (java.util.Date.)))))
    
    (messages [this] (rx/never))
    (menu [this] (rx/never))
    (unreadMessageCount [this] (rx.Observable/just 1))
    (unreadMessageCountReset [this])
    
    ))

(defn new-sneer [tuple-space own-prik followees]
  (let [own-puk (.publicKey own-prik)
        parties (atom {})
        profiles (atom {})
        puk->contact (atom (restore-contact-list tuple-space own-puk parties))
        ->contact-list (fn [contact-map] (->> contact-map vals (sort-by nickname) vec))
        contacts-subject (behavior-subject (->contact-list @puk->contact))]

    (rx/subscribe
      (->> 
        contacts-subject
        ;observe-for-computation
        flatmapseq
        (rx/flatmap #(.. % party publicKey observable)))
      (partial rx/on-next followees))
    
    (letfn [(duplicate-contact? [nickname party contact]
              (or (identical? party (.party contact))
                  (= nickname (.. contact nickname current))))

            (add-contact [nickname party]
              (->>
               (swap! puk->contact
                      (fn [cur]
                        (when (->> cur vals (some (partial duplicate-contact? nickname party)))
                          (throw (FriendlyException. "Duplicate contact!")))
                        (assoc cur
                          (party-puk party)
                          (reify-contact nickname party))))
               ->contact-list
               (rx/on-next contacts-subject)))]


      (reify Sneer

        (self [this]
          (new-party own-puk))

        (profileFor [this party]
          (produce-profile tuple-space profiles party))

        (contacts [this]
          (.asObservable contacts-subject))

        (addContact [this nickname party]
          (add-contact nickname party)
          (.. tuple-space
              publisher
              (audience own-puk)
              (type "sneer/contact")
              (field "party" (party-puk party))
              (pub nickname)))

        (findContact [this party]
          (get @puk->contact (party-puk party)))

        (conversationsContaining [this type]
          (rx/never))

        (conversations [this]
          (->>
            contacts-subject
            (rx/map (partial map #(reify-conversation (.party %))))))

        (produceParty [this puk]
          (produce-party parties puk))
        
        (tupleSpace [this]
          tuple-space)
        
        (produceConversationWith [this party] 
          (reify-conversation party))))))


(defprotocol Restartable
  (restart [this]))


(defn prepare-tuple-base [db]
  (try
		(persistence/create-tuple-table db)
    (catch Exception e))
  (try
		(persistence/create-prik-table db)
    (catch Exception e))
  (persistence/create db))

(defn new-sneer-admin

  ([own-prik network]
     (new-sneer-admin own-prik network (ReplaySubject/create)))

  ([own-prik network tuple-base]
     (let [puk (.publicKey own-prik)
           connection (connect network puk)
           followees (PublishSubject/create)
           tuple-space (core/reify-tuple-space puk tuple-base connection followees)
           sneer (new-sneer tuple-space own-prik followees)]
       (reify
         SneerAdmin
         (sneer [this] sneer)
         (privateKey [this] own-prik)
         Restartable
         (restart [this]
           (rx/on-completed connection)
           (new-sneer-admin own-prik network (restarted tuple-base)))))))

(defn produce-private-key [db]
  (if-let [existing (second (persistence/db-query db ["SELECT * FROM keys"]))]
    (sneer.impl.keys.Keys/createPrivateKey (first existing))
    (let [new-key (sneer.impl.keys.Keys/createPrivateKey)]
      (persistence/db-insert db :keys {"prik" (.bytes new-key)})
      new-key)))

(defn new-sneer-admin-over-db
  ([network db]
    (let [tuple-base (prepare-tuple-base db)
          own-prik (produce-private-key db)]
      (new-sneer-admin own-prik network tuple-base))))

