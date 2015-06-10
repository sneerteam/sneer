(ns sneer.flux
  (:require
    [clojure.core.async :refer [chan]])
  (:import
    [sneer.commons ActionBus]))

(definterface LeaseHolder
  (getLeaseChannel []))

(defn reify-LeaseHolder [_container]
  (let [lease (chan)]
    (reify LeaseHolder
      (getLeaseChannel [_] lease))))

(defn reify-ActionBus [_]
  (reify ActionBus
    (action [_ action] (println "Action: " action))))