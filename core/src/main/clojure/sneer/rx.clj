(ns sneer.rx
  (:require
    [clojure.core.async :as async :refer [<!!]]
    [rx.lang.clojure.core :as rx]
    [rx.lang.clojure.interop :as interop]
    [sneer.async :refer [tap-state sliding-chan decode-nil]])
  (:import [rx.subjects BehaviorSubject]
           [rx.schedulers Schedulers]
           [rx Observable Observable$OnSubscribe Subscriber Observer]
           [rx.functions FuncN]
           [java.util List]))

(defn on-subscribe [f]
  "Reifies a rx.Observable.OnSubscribe instance from a regular clojure function `f'."
  (reify Observable$OnSubscribe
    (call [_ subscriber] (f subscriber))))

(defn on-subscribe-for [^Observable observable]
  "Returns a rx.Observable.OnSubscribe instance that subscribes subscribers to `observable'."
  (on-subscribe
    (fn [^Subscriber subscriber]
      (.add subscriber (.subscribe observable subscriber)))))

(defn filter-by [criteria observable]
  "Filters an `observable' of maps by `criteria' represented as a map.
   Only maps containing all key/value pairs in criteria are kept."
  (let [ks (keys criteria)]
    (if ks
      (rx/filter #(= criteria (select-keys % ks)) observable)
      observable)))

(defn seq->observable [^Iterable iterable]
  (Observable/from iterable))

(defn atom->obs [atom]
  (let [subject (BehaviorSubject/create @atom)]
    (add-watch atom (Object.) (fn [_key _ref _old-value new-value]
                                (rx/on-next subject new-value)))
    (.asObservable subject)))

(defn flatmapseq [^Observable o]
  (.flatMapIterable o (interop/fn [seq] seq)))

(defn observe-for-computation [^Observable o]
  (.observeOn o (Schedulers/computation)))

(defn observe-for-io [^Observable o]
  (.observeOn o (Schedulers/io)))

(defn subscribe-on-io
  ([^Observable o on-next-action]
   (rx/subscribe
    (subscribe-on-io o)
    on-next-action))
  ([^Observable o]
   (rx/subscribe-on (Schedulers/io) o)))

(defn shared-latest [^Observable o]
  "Returns a `rx.Observable' that emits the latest value of o
   while sharing a single subscription as long as there are subscribers."
  (.. o (replay 1) refCount))

(defn latest [^Observable o]
  (doto (. o (replay 1))
    .connect))

(defn func-n [f]
  (reify FuncN
    (call [_ args] (f args))))

(defn combine-latest [f ^List os]
  (let [^FuncN fn (func-n f)]
    (Observable/combineLatest os fn)))

(defn map-some
  ([f ^Observable o]
    (map-some f nil o))
  ([f default ^Observable o]
   (rx/map #(if (some? %)
             (f %)
             default)
           o)))

(defn switch-map [f ^Observable o]
  (.switchMap o (interop/fn [x] (f x))))

(defn switch-map-some
  ([f ^Observable o]
    (switch-map-some f nil o))
  ([f default ^Observable o]
    (switch-map #(if (some? %)
                    (f %)
                    (rx/return default))
                o)))

(defn behavior-subject [& [initial-value]]
  (BehaviorSubject/create initial-value))

(defn close-on-unsubscribe!
  "Closes the channel when the subscriber is unsubscribed."
  [^Subscriber subscriber & chans]
  (.add subscriber (rx/subscription #(doseq [c chans] (async/close! c)))))

(defn pipe-to-subscriber!
  "Copies values from channel to rx subscriber in a separate thread."
  [chan ^Subscriber subscriber ^String thread-name]
  (async/thread                                             ;TODO: Revise: One thread per observable? How about using the rx computation pool or something? Klaus
    (.setName (Thread/currentThread) thread-name)
    (loop []
      (let [v (<!! chan)]
        (cond
          (nil? v) (rx/on-completed subscriber)
          (instance? Exception v) (rx/on-error subscriber v)
          :else (do
                  (try
                    (rx/on-next subscriber (decode-nil v))
                    (catch Exception e
                      (println "onNext Exception. subscriber:" subscriber "value:" v "thread:" thread-name)
                      (.printStackTrace e)))
                  (recur)))))))

(defn obs-tap [state-machine debug-name & [xform]]
  (rx/observable*
    (fn [^Subscriber subscriber]
      (let [tap (tap-state state-machine (sliding-chan 1 xform))]
        (close-on-unsubscribe! subscriber tap)
        (pipe-to-subscriber! tap subscriber debug-name)))))
