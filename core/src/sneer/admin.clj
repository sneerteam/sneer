(ns sneer.admin
  (:require
    [rx.lang.clojure.core :as rx])
  (:import
    [sneer.admin SneerAdmin]
    [sneer Sneer PrivateKey Party Contact]
    [sneer.rx ObservedSubject]
    [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
    [rx.schedulers TestScheduler]
    [rx.subjects ReplaySubject PublishSubject]))

(defn new-party [puk]
  (reify Party
    (publicKey [this]
      (.observed (ObservedSubject/create puk)))))

(defn party-puk [party]
  (.. party publicKey current))

(defn reify-contact [nickname party]
  (let [nickname-subject (ObservedSubject/create nickname)]
    (reify Contact
      (party [this] party)
      (nickname [this] (.observed nickname-subject)))))

(defn new-sneer [tuple-space own-prik]
  (let [parties (atom {})
        puk->contact (atom {})
        own-puk (.publicKey own-prik)]

    (letfn [(produce-party [puk]
              (->
               (swap! parties update-in [puk] #(if (nil? %) (new-party puk) %))
               (get puk)))

            (restore-contact-from-tuple [tuple]
              (let [party-puk (.get tuple "party")
                    party (produce-party party-puk)
                    nickname (.payload tuple)]
                (swap! puk->contact assoc party-puk (reify-contact nickname party))))

            (duplicate-contact? [nickname party contact]
              (or (identical? party (.party contact))
                  (= nickname (.. contact nickname current))))

            (add-contact [nickname party]
              (swap! puk->contact
                     (fn [cur]
                       (assert (->> cur vals (not-any? (partial duplicate-contact? nickname party)))
                               "Duplicate contact!")
                       (assoc cur (party-puk party)
                         (reify-contact nickname party)))))]

      (->
       (.. tuple-space
           filter
           (author own-puk)
           (type "sneer/contact")
           localTuples)
       (rx/subscribe restore-contact-from-tuple))

      (reify Sneer

        (self [this]
          (new-party own-puk))

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
          (produce-party puk))))))

(defn new-sneer-admin [tuple-space own-prik]
  (let [sneer (new-sneer tuple-space own-prik)]
    (reify SneerAdmin
      (sneer [this] sneer))))
