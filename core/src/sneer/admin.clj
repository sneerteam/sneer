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

(defn new-sneer-admin [tuple-space own-prik]
  (let [sneer
        (reify Sneer
          (self [this]
            (new-party (.publicKey own-prik)))
          (produceParty [this puk]
            (new-party puk)))]
	  (reify SneerAdmin
	    (sneer [this] sneer))))
