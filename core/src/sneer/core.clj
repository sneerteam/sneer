(ns sneer.core
  (:import [sneer.admin SneerAdmin]
           [sneer Sneer PrivateKey]
           [sneer.tuples Tuple Tuples TuplePublisher TupleSubscriber]
           [rx.subjects ReplaySubject]))

(def tuples (ReplaySubject/create))

(defn ->tuple [attrs]
  (reify Tuple))

(defn new-tuple-publisher
  ([] (new-tuple-publisher {}))
  ([attrs]
    (reify TuplePublisher
      (intent [this intent]
        (new-tuple-publisher (assoc attrs "intent" intent)))
      (pub [this value]
        (. tuples onNext (->tuple attrs))
        this))))

(defn new-tuple-subscriber []
  (reify TupleSubscriber
    (tuples [this]
      tuples)))

(defn new-tuples []
  (reify Tuples
    (newTuplePublisher [this]
      (new-tuple-publisher))
    (newTupleSubscriber [this]
      (new-tuple-subscriber))))

(defn new-sneer-admin []
  (reify SneerAdmin
    (initialize [this pk]
      (reify Sneer
        (tuples [this]
          (new-tuples))))))

