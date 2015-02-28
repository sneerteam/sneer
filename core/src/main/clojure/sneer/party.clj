(ns sneer.party
  (:require
   [sneer.commons :refer [produce!]])
  (:import
   [sneer Party PublicKey]
   [sneer.rx ObservedSubject]))

(defprotocol PartyImpl
  (name-subject [this]))

(defn reify-party [puk]
  (let [name (ObservedSubject/create (str "? PublicKey: " (-> ^PublicKey puk .toHex (subs 0 7)) "..."))]
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
    #(produce! reify-party puk->party %)))
