(ns sneer.admin
  (:require
    [rx.lang.clojure.core :as rx])
  (:import
    [sneer.admin SneerAdmin]
    [sneer Sneer PrivateKey Party Contact]
    [sneer.rx ObservedSubject]
    [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
    [rx.schedulers TestScheduler]
    [rx.subjects BehaviorSubject]))

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


(defn reify-contact [nickname party]
  (let [nickname-subject (ObservedSubject/create nickname)]
    (reify Contact
      (party [this] party)
      (nickname [this] (.observed nickname-subject))
      (toString [this] (str "#<Contact " nickname ">")))))

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
        contacts-subject (BehaviorSubject/create (->contact-list @puk->contact))]

    (letfn [(duplicate-contact? [nickname party contact]
              (or (identical? party (.party contact))
                  (= nickname (.. contact nickname current))))

            (add-contact [nickname party]
              (->>
               (swap! puk->contact
                      (fn [cur]
                        (assert (->> cur vals (not-any? (partial duplicate-contact? nickname party)))
                                "Duplicate contact!")
                        (assoc cur (party-puk party)
                               (reify-contact nickname party))))
               ->contact-list
               (rx/on-next contacts-subject)))]


      (reify Sneer

        (self [this]
          (new-party own-puk))

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

(defn new-sneer-admin [tuple-space own-prik]
  (let [sneer (new-sneer tuple-space own-prik)]
    (reify SneerAdmin
      (sneer [this] sneer))))
