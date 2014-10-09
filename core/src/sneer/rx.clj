(ns sneer.rx
  (:require 
    [rx.lang.clojure.core :as rx]
    [rx.lang.clojure.interop :as interop])
  (:import [rx.subjects Subject BehaviorSubject]
           [rx.schedulers Schedulers]))

(defn on-subscribe [f]
  "Reifies a rx.Observable.OnSubscribe instance from a regular clojure function `f'."
  (reify rx.Observable$OnSubscribe
    (call [this subscriber] (f subscriber))))

(defn on-subscribe-for [^rx.Observable observable]
  "Returns a rx.Observable.OnSubscribe instance that subscribes subscribers to `observable'."
  (on-subscribe
    (fn [^rx.Subscriber subscriber]
      (.add subscriber (.subscribe observable subscriber)))))

(defn subject* [^rx.Observable observable ^rx.Observer observer]
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

(defn atom->observable [atom]
    (let [subject (BehaviorSubject/create @atom)]
      (add-watch atom (Object.) (fn [_key _ref _old-value new-value]
                                (rx/on-next subject new-value)))
      (.asObservable subject)))

(defn flatmapseq [^rx.Observable o]
  (.flatMapIterable o (interop/fn [seq] seq)))

(defn observe-for-computation [^rx.Observable o]
  (.observeOn o (Schedulers/computation)))

(defn observe-for-io [^rx.Observable o]
  (.observeOn o (Schedulers/io)))

(defn subscribe-on-io [^rx.Observable o on-next-action]
  (rx/subscribe
    (rx/subscribe-on (rx.schedulers.Schedulers/io) o)
    on-next-action))

(defn shared-latest [^rx.Observable o]
  "Returns a `rx.Observable' that publishes the latest value of the source sequence
   while sharing a single subscription as long as there are subscribers."
  (.. o (replay 1) refCount))
