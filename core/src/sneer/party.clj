(ns sneer.party
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [subject* shared-latest]]
   [sneer.commons :refer [produce]])
  (:import
   [sneer Profile Party]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace]
   [rx.subjects Subject]))

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
