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
    (tuple-getter author)
    (tuple-getter value)))

(defmacro publisher-attr [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))

(defn new-tuple-publisher
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

(defmacro subscriber-filter [attr]
  `(~attr [~'this ~'expected]
     (new-tuple-subscriber
       (rx/filter #(= (. % ~attr) ~'expected) ~'tuples))))

(defn new-tuple-subscriber [tuples]
  (reify+ TupleSubscriber
    (subscriber-filter intent)
    (subscriber-filter author)
    (tuples [this]
      tuples)))

(defn new-tuples [own-puk tuples-out tuples-in]
  (reify Tuples
    (newTuplePublisher [this]
      (new-tuple-publisher tuples-out {"author" own-puk}))
    (newTupleSubscriber [this]
      (new-tuple-subscriber tuples-in))))

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
            (new-tuples puk tuples (rx/filter (visible-to puk) tuples))))))))

(defn new-session []
  (ReplaySubject/create))

