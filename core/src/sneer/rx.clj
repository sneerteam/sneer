(ns sneer.rx
  (:require 
    [rx.lang.clojure.core :as rx]
    [rx.lang.clojure.interop :as interop])
  (:import [rx.subjects Subject]
           [rx.schedulers Schedulers]))

(defn on-subscribe [f]
  "Reifies a rx.Observable.OnSubscribe instance from a regular clojure function `f'."
  (reify rx.Observable$OnSubscribe
    (call [this subscriber] (f subscriber))))

(defn on-subscribe-for [observable]
  "Returns a rx.Observable.OnSubscribe instance that subscribes subscribers to `observable'."
  (on-subscribe
    (fn [subscriber]
      (.add subscriber (.subscribe observable subscriber)))))

(defn subject* [observable observer]
  "Creates a rx.Subject from an `observable' part and an `observer' part."
  (let [subscriber (on-subscribe-for observable)]
    (proxy [Subject] [subscriber]
      (onNext [value] (.onNext observer value))
      (onError [error] (.onError observer error))
      (onCompleted [] (.onCompleted observer)))))

(defn filter-by [criteria observable]
  "Filters an `observable' of maps by `criteria' represented as a map.
Only maps containing all key/value pairs in criteria are kept."
  (let [ks (keys criteria)]
    (if ks
      (rx/filter #(= criteria (select-keys % ks)) observable)
      observable)))

(defn seq->observable [^java.lang.Iterable iterable]
  (rx.Observable/from iterable))

(defn flatmapseq [^rx.Observable o]
  (.flatMapIterable o (interop/fn [seq] seq)))

(defn observe-for-computation [o]
  (.observeOn o (Schedulers/computation)))
