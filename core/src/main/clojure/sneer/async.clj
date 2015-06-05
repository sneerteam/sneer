(ns sneer.async
  (:require [clojure.core.async :as async :refer [chan go remove< >! <! <!! tap]]
            [rx.lang.clojure.core :as rx]
            [clojure.stacktrace :refer [print-throwable]]
            [sneer.commons :refer :all]))

(def IMMEDIATELY (doto (async/chan) async/close!))

(defn dropping-chan [& [n]]
  (chan (async/dropping-buffer (or n 1))))

(defn sliding-chan [& [n]]
  (chan (async/sliding-buffer (or n 1))))

(defn connection [in out]
  [in out])
(defn in [connection]
  (first connection))
(defn out [connection]
  (second connection))
(defn other-side [[in out]]
  [out in])

(defmacro go-trace
  [& forms]
  `(go
     (try
       ~@forms
       (catch Throwable ~'e
         (println "GO ERROR" ~'e)
         #_(print-throwable ~'e)
         (.printStackTrace ~'e)))))

(defmacro go-loop-trace
  "Same as go-loop but prints unhandled exception stack trace"
  [binding & forms]
  `(go-trace
    (loop ~binding
      ~@forms)))

(defmacro go-while-let
  "Makes it easy to continue processing data from a channel until it closes"
  [binding & forms]
  `(go-trace
     (while-let ~binding
                ~@forms)))

(defn non-repeating<
  ([ch]
    (non-repeating< = ch))
  ([equality-fn ch]
    (let [ret (chan)]
      (go-trace
        (loop [previous nil]
          (when-some [v (<! ch)]
                     (when-not (equality-fn v previous)
                       (>! ret v))
                     (recur v))))
      ret)))

(defn dropping-tap [mult]
  (tap mult (dropping-chan)))

(defn link-chan-to-subscriber
  "Closes the channel when the subscriber is unsubscribed."
  [chan ^rx.Subscriber subscriber]
  (.add subscriber (rx/subscription #(async/close! chan))))

(defn thread-chan-to-subscriber
  "Copies values from channel to rx subscriber in a separate thread."
  [chan ^rx.Subscriber subscriber ^String thread-name]
  (async/thread
    (.setName (Thread/currentThread) thread-name)
    (while-let [value (<!! chan)]
      (try
        (rx/on-next subscriber value)
        (catch Exception e
          (throw (RuntimeException. (str "onNext Exception. subscriber: " subscriber " value: " value "thread: " thread-name)
                                    e)))))
    (rx/on-completed subscriber)))
