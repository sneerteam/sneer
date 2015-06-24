(ns sneer.flux
  (:require
    [clojure.core.async :refer [chan mult tap go >!]]
    [sneer.async :refer [close-with!]])
  (:import
    [sneer.flux Dispatcher Action]))

(defprotocol ActionSource
  (tap-actions [_ ch]))

(defn reify-Dispatcher [container]
  (let [actions (chan)
        mult (mult actions)
        lease (.produce container :lease)]

    (close-with! lease actions)

    (reify
      Dispatcher
      (dispatch [_ action] (go (>! actions action)))

      ActionSource
      (tap-actions [_ ch] (tap mult ch)))))

(defn of-type [type]
  (fn [^Action action] (-> action .type (= type))))
