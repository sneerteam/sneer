(ns sneer.core
  (:require [rx.lang.clojure.core :as rx])
  (:import [sneer.admin SneerAdmin]
           [sneer Sneer PrivateKey]
           [sneer.tuples Tuple Tuples TuplePublisher TupleSubscriber]
           [rx.subjects ReplaySubject]))

(defmacro reify+
  "expands to reify form after macro expanding the body"
  [& body]  
  `(reify ~@(map macroexpand body)))

(defmacro tuple-getter [g]
  `(~g [~'this]
       (get ~'attrs ~(name g))))

(defn ->tuple [attrs]
  (reify+ Tuple
    (tuple-getter intent)
    (tuple-getter audience)
    (tuple-getter value)))

(defmacro publisher-attr [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))

(defn new-tuple-publisher
  ([tuples] (new-tuple-publisher tuples {}))
  ([tuples attrs]
    (letfn
      [(with [attr value]
          (new-tuple-publisher tuples (assoc attrs attr value)))]
      (reify+ TuplePublisher
        (publisher-attr intent)
        (publisher-attr audience)
        (publisher-attr value)
        (pub [this value]
           (.. this (value value) pub))
        (pub [this]
           (. tuples onNext (->tuple attrs))
           this)))))

(defn new-tuple-subscriber [tuples]
  (reify TupleSubscriber
    (intent [this expected]
      (new-tuple-subscriber
        (rx/filter #(= (. % intent) expected) tuples)))
    (tuples [this]
      tuples)))

(defn new-tuples [all-tuples my-tuples]
  (reify Tuples
    (newTuplePublisher [this]
      (new-tuple-publisher all-tuples))
    (newTupleSubscriber [this]
      (new-tuple-subscriber my-tuples))))

(defn visible-to [puk]
  (fn [tuple]
    (let [audience (. tuple audience)]
      (or
        (nil? audience)
        (= audience puk)))))

(defn new-sneer-admin [tuples]
  (reify SneerAdmin
    (initialize [this prik]
      (let [puk (. prik publicKey)]
        (reify Sneer
          (tuples [this]
            (new-tuples tuples (rx/filter (visible-to puk) tuples))))))))

(defn new-session []
  (ReplaySubject/create))

