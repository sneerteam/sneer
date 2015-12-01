(ns sneer.rx-test-util
  (:require
    [clojure.core.async :refer [>!! <!! close! chan]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [decode-nil]]
    [sneer.commons :refer [nvl]]
    [sneer.rx :refer [observe-for-io subscribe-on-io]]
    [sneer.test-util :refer [<wait-trace! <!!? closes]]
    )
  (:import [rx Observable]))

(defn subscribe-chan [c observable]
  (rx/subscribe observable
                #(>!! c (nvl % :nil))  ; Channels cannot take nil
                #(do
                  #_(.printStackTrace %)
                  (>!! c {::error %})
                  (close! c))
                #(close! c)))

(defn observable->chan
  ([obs]
   (doto (chan)
     (subscribe-chan obs)))
  ([obs xform]
   (doto (chan 1 xform)
     (subscribe-chan obs))))

(defn ->chan2
  ([obs]
   (observable->chan (subscribe-on-io obs)))
  ([obs xform]
   (observable->chan (subscribe-on-io obs) xform)))

(defn ->chan [^Observable o]
  (->> o observe-for-io observable->chan))

(defn emits [expected]
  (fn [obs]
    (let [ch (->chan obs)]
      (<wait-trace! ch expected))))

(defn emits-error [exception-type]
  (emits #(instance? exception-type (::error %))))

(defn <next [obs]
  (decode-nil (<!!? (->chan obs))))

(defn completes [obs]
  (closes (->chan2 obs)))

; (do (require 'midje.repl) (midje.repl/autotest))
