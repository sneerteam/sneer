(ns sneer.async
  (:require [clojure.core.async :as async :refer [chan go >! <! <!! alt! alts! timeout mult tap close!]]
            [rx.lang.clojure.core :as rx]
            [clojure.stacktrace :refer [print-throwable]]
            [sneer.commons :refer :all])
  (:import [rx Subscriber]))

(def closed-chan (doto (async/chan) async/close!))
(def IMMEDIATELY closed-chan)

(defn close-with!
  "Closes victim channel when ch emits a value."
  [ch victim]
  (go
    (<! ch)
    (close! victim)))

(defn dropping-chan [& [n xform]]
  (chan (async/dropping-buffer (or n 1)) xform))

(defn sliding-chan [& [n xform]]
  (chan (async/sliding-buffer (or n 1)) xform))

(defn sliding-tap [mult]
  (let [ch (sliding-chan)] (async/tap mult ch) ch))

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

(defn dropping-tap [mult]
  (tap mult (dropping-chan)))

(defn close-on-unsubscribe!
  "Closes the channel when the subscriber is unsubscribed."
  [^Subscriber subscriber & chans]
  (.add subscriber (rx/subscription #(doseq [c chans] (async/close! c)))))

(defn pipe-to-subscriber!
  "Copies values from channel to rx subscriber in a separate thread."
  [chan ^Subscriber subscriber ^String thread-name]
  (async/thread
    (.setName (Thread/currentThread) thread-name)
    (while-let [value (<!! chan)]
      (try
        (rx/on-next subscriber value)
        (catch Exception e
          (throw (RuntimeException. (str "onNext Exception. subscriber: " subscriber " value: " value "thread: " thread-name)
                                    e)))))
    (rx/on-completed subscriber)))

(defn republish-latest-every! [period in out] ; This republish fn would be cool as a transducer. :)
  (go-loop-trace [latest nil
                  period-timeout (chan)]
    (alt! :priority true
          in
          ([latest]
            (when latest
              (when (>! out latest)
                (recur latest (timeout period)))))

          period-timeout
          ([_]
            (>! out latest)
            (recur latest (timeout period))))))

(defn state-machine
  "Returns a channel that accepts other channels as taps for this state machine
   in a way similar to clojure.core.async/tap.
   Reduces initial-state applying (function state event) to each event from the
   events-in channel and puts each resulting state onto the tap channels."
  ([f initial-state events-in]
   (let [no-history closed-chan]
     (state-machine f initial-state no-history events-in)))

  ([f initial-state event-history events-in]
   (let [taps-in (chan)
         states-out (chan)
         mult (mult states-out)]

     (go-trace
       (loop [state initial-state
              inputs [taps-in event-history]
              current? false]
         (let [[v ch] (alts! inputs :priority true)]
           (condp = ch

             taps-in
             (when-some [tap v]
               (when current?
                 (>! tap state))
               (async/tap mult tap)
               (recur state inputs current?))

             event-history
             (if-some [event v]
               (recur (f state event) inputs false)
               (do
                 (>! states-out state)
                 (recur state [taps-in events-in] true)))

             events-in
             (when-some [event v]
               (let [state' (f state event)]
                 (>! states-out state')
                 (recur state' inputs true))))))

       (close! taps-in)
       (close! states-out))

     taps-in)))

(defn tap-state
  ([machine]
   (let [result (sliding-chan)]
     (tap-state machine result)
     result))
  ([machine tap-ch]
   (go
     (>! machine tap-ch))
    tap-ch))

(defn peek-state! [machine]
  (go
    (let [tap (tap-state machine)
          result (<! tap)]
      (close! tap)
      result)))

(defn with-nil [v]
  (if (= v :nil) nil v))

(defn wait-for! [ch pred]
  (go-loop-trace []
    (when-let [v (<! ch)]
      (if (pred v)
        v
        (recur)))))