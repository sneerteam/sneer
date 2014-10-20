(ns sneer.party
  (:require
   [sneer.commons :refer [produce!]])
  (:import
   [sneer Party]
   [sneer.rx ObservedSubject]))

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

(defn produce-party! [parties puk]
  (parties puk))

(defn create-puk->party []
  (let [puk->party (atom {})]
    #(produce! new-party puk->party %)))
