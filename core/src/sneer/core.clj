(ns sneer.core
  (:require [rx.lang.clojure.core :as rx])
  (:import [sneer.admin SneerAdmin]
           [sneer Sneer PrivateKey]
           [sneer.tuples Tuple Tuples TuplePublisher TupleSubscriber]
           [rx.subjects ReplaySubject]))

(defn ->tuple [attrs]
  (reify Tuple
    (intent [this]
      (get attrs "intent"))
    (value [this]
      (get attrs "value"))))

(defn new-tuple-publisher
  ([tuples] (new-tuple-publisher tuples {}))
  ([tuples attrs]
    (letfn
      [(with [attr value]
          (new-tuple-publisher tuples (assoc attrs attr value)))]
      (reify TuplePublisher
        (intent [this intent]
          (with "intent" intent))
        (audience [this audience]
          (with "audience" audience))
        (pub [this value]
          (. tuples onNext (->tuple (assoc attrs "value" value)))
          this)))))

(defn new-tuple-subscriber [tuples]
  (reify TupleSubscriber
    (intent [this expected]
      (new-tuple-subscriber
        (rx/filter #(= (. % intent) expected) tuples)))
    (tuples [this]
      tuples)))

(defn new-tuples [tuples]
  (reify Tuples
    (newTuplePublisher [this]
      (new-tuple-publisher tuples))
    (newTupleSubscriber [this]
      (new-tuple-subscriber tuples))))

(defn new-sneer-admin [tuples]
  (reify SneerAdmin
      (initialize [this pk]
        (reify Sneer
          (tuples [this]
            (new-tuples tuples))))))

(defn new-session []
  (ReplaySubject/create))

