(ns sneer.flux
  (:require
    [clojure.core.async :refer [chan close! mult tap go >! <!]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [close-with! go-trace]])
  (:import
    [rx.subjects AsyncSubject]
    [sneer.commons Container]
    [sneer.flux Dispatcher Action Request ActionBase]))

(defprotocol ActionSource
  (tap-actions [_ ch]))

(defn- ->map [^ActionBase a]
  (assoc (apply hash-map (.keyValuePairs a))
         :type (.type a)))

(defn reify-Dispatcher [^Container container]
  (let [actions (chan) #_(chan 1 (map #(do (println "ACTION:" %) %)))
        mult (mult actions)
        lease (.produce container :lease)]

    (close-with! lease actions)

    (reify
      Dispatcher
      (dispatch [_ action]
        (go-trace (>! actions (->map action))))

      (request [_ request]
        (let [response (chan 1)
              subject (AsyncSubject/create)]

          (go-trace
            (>! actions (assoc (->map request) ::response response))
            (rx/on-next subject (<! response))
            (close! response)
            (rx/on-completed subject))

          subject))

      ActionSource
      (tap-actions [_ ch] (tap mult ch)))))

(defn of-type [type]
  (fn [action] (-> action :type (= type))))

(defn response [request]
  (request ::response))
