(ns sneer.admin
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [subject*]]
   [sneer.core :as core :refer [connect dispose restarted]]
   [sneer.persistent-tuple-base :as persistence]
   [clojure.java.io :as io])
  (:import
   [sneer.admin SneerAdmin]
   [sneer.commons.exceptions FriendlyException]
   [sneer Sneer PrivateKey Party Contact Profile]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
   [rx.schedulers TestScheduler]
   [rx.subjects BehaviorSubject ReplaySubject]))

(defn behavior-subject [& [initial-value]]
  (BehaviorSubject/create initial-value))

(defn replay-last-subject []
  (ReplaySubject/createWithSize 1))

(defn new-party [puk]
  (reify Party
    (publicKey [this]
      (.observed (ObservedSubject/create puk)))))

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

(defn reify-contact [nickname party]
  (let [nickname-subject (ObservedSubject/create nickname)]
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

(defn new-sneer [tuple-space own-prik]
  (let [own-puk (.publicKey own-prik)
        parties (atom {})
        puk->contact (atom (restore-contact-list tuple-space own-puk parties))
        ->contact-list (fn [contact-map] (->> contact-map vals (sort-by nickname) vec))
        contacts-subject (behavior-subject (->contact-list @puk->contact))]

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
          (reify-profile party tuple-space))

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

        (produceParty [this puk]
          (produce-party parties puk))))))


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
           followees (rx.Observable/never)
           tuple-space (core/reify-tuple-space puk tuple-base connection followees)
           sneer (new-sneer tuple-space own-prik)]
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

