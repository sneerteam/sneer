(ns sneer.rx
  (:require [rx.lang.clojure.core :as rx])
  (:import [rx.subjects Subject]))

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


