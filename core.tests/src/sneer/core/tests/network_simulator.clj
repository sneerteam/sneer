(ns sneer.core.tests.network-simulator
  (:require [rx.lang.clojure.core :as rx])
  (:import [rx.subjects PublishSubject]))

(defn new-network []
  (PublishSubject/create))

(defn stop-network [network]
  (rx/on-completed network))
