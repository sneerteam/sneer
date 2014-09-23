(ns sneer.party
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [subject* shared-latest]]
   [sneer.commons :refer [produce]])
  (:import
   [sneer Party]
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
