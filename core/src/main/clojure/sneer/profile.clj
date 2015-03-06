(ns sneer.profile
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [subject* shared-latest]]
   [sneer.commons :refer [produce!]]
   [sneer.party :refer [party-puk]])
  (:import
   [sneer Profile]
   [sneer.tuples Tuple TupleSpace]
   [rx.subjects Subject]))

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
                 (onNext [_ value]
                   (publish value))))))

          (local-payloads-of [type]
            (.. tuple-space
                filter
                (author (party-puk party))
                (type type)
                localTuples
                toBlocking
                (firstOrDefault nil)))]

    (let [^Subject preferred-nickname (payload-subject "profile/preferred-nickname")
          ^Subject own-name (payload-subject "profile/own-name")
          ^Subject selfie (payload-subject "profile/selfie")
          ^Subject city (payload-subject "profile/city")
          ^Subject country (payload-subject "profile/country")]

      (reify Profile
        (ownName [_]
          (.asObservable own-name))
        (setOwnName [_ value]
          (rx/on-next own-name value))
        (selfie [_]
          (.asObservable selfie))
        (setSelfie [_ value]
          (rx/on-next selfie value))
        (preferredNickname [_]
          (.asObservable preferred-nickname))
        (setPreferredNickname [_ value]
          (rx/on-next preferred-nickname value))
        (city [_]
          (.asObservable city))
        (setCity [_ value]
          (rx/on-next city value))
        (country [_]
          (.asObservable country))
        (setCountry [_ value]
          (rx/on-next country value))
        (isOwnNameLocallyAvailable [_]
          (some? (local-payloads-of "profile/own-name")))))))

(defn produce-profile [tuple-space profiles party]
  (produce! #(reify-profile % tuple-space) profiles party))
