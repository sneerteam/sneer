(ns sneer.party
  (:require
   [sneer.commons :refer [produce!]]
   [sneer.party-impl :refer :all])
  (:import
   [sneer Party PublicKey]
   [sneer.rx ObservedSubject]))

(defn reify-party [puk]
  (let [name (ObservedSubject/create (str "? PublicKey: " (-> ^PublicKey puk .toHex (subs 0 7)) "..."))]
    (reify
      Party
        (name [_]
          (.observable name))
        (publicKey [_]
          (.observed (ObservedSubject/create puk)))
        (toString [_]
          (str "#<Party " puk ">"))
      PartyImpl
        (name-subject [_] name))))

(defn party->puk [^Party party]
  (some-> party .publicKey .current))

(defn produce-party! [parties puk]
  (parties puk))

(defn create-puk->party []
  (let [puk->party (atom {})]
    #(produce! reify-party puk->party %)))
