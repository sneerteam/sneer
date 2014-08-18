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

(defn new-sneer [own-prik]
  (let [parties (atom {})]
	  (reify Sneer
	    (self [this]
	      (new-party (.publicKey own-prik)))
	    (produceParty [this puk]
       (let [new-parties
             (swap! parties (fn [cur] 
                              (if (get cur puk)
                                cur
                                (assoc cur puk (new-party puk)))))]
         (get new-parties puk))))))

(defn new-sneer-admin [tuple-space own-prik]
  (let [sneer (new-sneer own-prik)]
	  (reify SneerAdmin
	    (sneer [this] sneer))))
