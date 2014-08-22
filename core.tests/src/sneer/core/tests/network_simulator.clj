(ns sneer.core.tests.network-simulator
  (:require [rx.lang.clojure.core :as rx]
            [sneer.core :as core]
            [sneer.rx :refer [subject*]])
  (:import [rx.subjects Subject PublishSubject]))

(defn addressed-to [puk]
  (fn [envelope]
    (let [destination-address (:address envelope)]
      (= destination-address puk))))

(extend-type Subject
  core/Network
  (connect [network own-puk]
    (let [envelopes-for-me (rx/filter (addressed-to own-puk) network)]
      (subject* envelopes-for-me network)))
  core/Disposable
  (dispose [network]
    (rx/on-completed network)))

(defn new-network []
  (PublishSubject/create))
