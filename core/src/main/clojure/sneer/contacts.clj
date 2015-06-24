(ns sneer.contacts
  (:require
    [clojure.core.async :refer [chan <!]]
    [sneer.async :refer [go-while-let]]
    [sneer.flux :refer [tap-actions of-type]])
  (:import
    [sneer.flux Dispatcher]))

(defn start! [container]
  (let [actions (chan 1 (filter (of-type "set-nickname")))]
    (tap-actions (.produce container Dispatcher) actions)
    (go-while-let [a (<! actions)]


      )))
