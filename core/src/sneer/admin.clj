(ns sneer.admin
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.core :as core :refer [connect dispose]])
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
  ;; Observable<String> ownName();
  ;; void setOwnName(String newOwnName);

  ;; Observable<String> preferredNickname();
  ;; void setPreferredNickname(String newPreferredNickname);

  ;; Observable<byte[]> selfie();
  ;; void setSelfie(byte[] newSelfie);

  ;; Observable<String> country();
  ;; void setCountry(String newCountry);

  ;; Observable<String> city();
  ;; void setCity(String newCity);

  (letfn [(payloads-of [type]
            (rx/map
             #(.payload %)
             (.. tuple-space
                 filter
                 (type type)
                 (author (party-puk party))
                 tuples)))]

    (let [nickname (behavior-subject)]

      (rx/subscribe (payloads-of "profile/preferred-nickname")
                    (partial rx/on-next nickname))

      (reify Profile
        (preferredNickname [this]
          (.asObservable nickname))
        (setPreferredNickname [this value]
          (.. tuple-space
              publisher
              (type "profile/preferred-nickname")
              (pub value)))))))

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

        (produceParty [this puk]
          (produce-party parties puk))))))


(defprotocol Restartable
  (restart [this]))


(defn copy-of [observable]
  (let [copy (ReplaySubject/create)]
    (rx/subscribe observable #(rx/on-next copy %))
    copy))

(defn new-sneer-admin

  ([own-prik network]
     (new-sneer-admin own-prik network (ReplaySubject/create)))

  ([own-prik network local-tuples]
     (let [puk (.publicKey own-prik)
           connection (connect network puk)
           tuple-space (core/reify-tuple-space puk (rx.Observable/never) connection local-tuples)
           sneer (new-sneer tuple-space own-prik)]
       (reify
         SneerAdmin
         (sneer [this] sneer)
         Restartable
         (restart [this]
           (rx/on-completed connection)
           (rx/on-completed local-tuples)
           (new-sneer-admin own-prik network (copy-of local-tuples)))))))
