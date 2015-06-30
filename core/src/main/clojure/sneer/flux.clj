(ns sneer.flux
  (:require
    [clojure.core.async :refer [chan close! mult tap go >!! <!]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [close-with! go-trace decode-nil]]
    [sneer.test-util :refer [<!!? ->chan]])
  (:import
    [rx.subjects AsyncSubject]
    [sneer.commons Container]
    [sneer.flux Dispatcher Action Request ActionBase]))

(defprotocol ActionSource
  (tap-actions [_ ch]))

(defn- ->map [^ActionBase a]
  (assoc (apply hash-map (.keyValuePairs a))
         :type (.type a)))

(defn emit [subject res]
  (if (instance? Exception res)
    (rx/on-error subject res)
    (do
      (rx/on-next subject res)
      (rx/on-completed subject))))

(defn reify-Dispatcher [^Container container]
  (let [actions (chan) #_(chan 1 (map #(do (println "ACTION:" %) %)))
        mult (mult actions)
        lease (.produce container :lease)]

    (close-with! lease actions)

    (reify
      Dispatcher
      (dispatch [this action]
        (>!! actions (->map action)))

      (request [_ request]
        (let [response (chan 1)
              subject (AsyncSubject/create)]

          (>!! actions (assoc (->map request) ::response response))
          (go-trace
            (let [res (with-nil (<! response))]
              (close! response)
              (emit subject res)))

          subject))

      ActionSource
      (tap-actions [_ ch] (tap mult ch)))))

(defn of-type [type]
  (fn [action] (-> action :type (= type))))

(defn response [request]
  (request ::response))

(defn action [type & keyValuePairs]
  (Action/action type (into-array Object keyValuePairs)))
(defn request [type & keyValuePairs]
  (Request/request type (into-array Object keyValuePairs)))
