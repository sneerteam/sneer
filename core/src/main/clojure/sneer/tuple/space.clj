(ns sneer.tuple.space
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.async :refer [go-trace thread-chan-to-subscriber]]
   [sneer.serialization :refer [roundtrip]]
   [sneer.commons :refer [now reify+ while-let]]
   [clojure.core.async :refer [thread chan <! <!! close!]]
   [sneer.tuple.persistent-tuple-base :refer [last-by-id]]
   [sneer.tuple.protocols :refer [store-tuple query-tuples]]
   [sneer.tuple.macros :refer :all])
  (:import
    [rx.subjects AsyncSubject]
    [sneer PrivateKey PublicKey]
    [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
    [rx Subscriber Observable]))

(defn reify-tuple [tuple]
  (assert (some? (get tuple "timestamp")))
  (reify+ Tuple
    (get [this key] (get tuple key))
    (tuple-getter type)
    (tuple-getter audience)
    (tuple-getter author)
    (tuple-getter payload)
    (tuple-getter timestamp)
    (toString [this] (str tuple))
    (entrySet [this] (.entrySet tuple))))

(defn payload [^Tuple tuple]
  (.payload tuple))

(def max-size 1000)
(defn- timestamped [proto-tuple]
  (roundtrip (assoc proto-tuple "timestamp" (now)) max-size))

(defn new-tuple-publisher [tuples-out proto-tuple]
  (letfn
    [(with [field value]
       (new-tuple-publisher tuples-out (assoc proto-tuple field value)))]
    (reify+ TuplePublisher
      (with-field type)
      (with-field audience)
      (with-field payload)
      (field [this field value]
        (with field value))
      (pub [this payload]
        (.. this (payload payload) pub))
      (pub [this]
        (let [tuple (timestamped proto-tuple)
              result (AsyncSubject/create)
              result-chan (store-tuple tuples-out tuple)]
          (go-trace
            (when-some [tuple (<! result-chan)]
              (rx/on-next result (reify-tuple tuple)))
            (rx/on-completed result))
          result)))))

(defn- set-thread-name! [name]
  (.setName (Thread/currentThread) name))

(defn rx-query-tuples [tuple-base criteria keep-alive]
  (rx/observable*
   (fn [^Subscriber subscriber]
     (let [result (chan)]
       (if keep-alive
         (let [lease (chan)]
           (query-tuples tuple-base criteria result lease)
           (.add subscriber (rx/subscription #(do (close! lease) (close! result)))))
         (query-tuples tuple-base criteria result))
       ;; TODO: reassess use of thread here
       (thread-chan-to-subscriber result subscriber (str "tuple-query: " criteria))))))

(defn new-tuple-filter
  ([tuple-base own-puk] (new-tuple-filter tuple-base own-puk {}))
  ([tuple-base own-puk criteria]
    (letfn
        [(with [field value]
           (new-tuple-filter tuple-base own-puk (assoc criteria field value)))]

        (reify+ TupleFilter
          (with-field type)
          (with-field author)
          (^TupleFilter audience [this ^PrivateKey prik]
            (with "audience" (.publicKey prik)))
          (^TupleFilter audience [this ^PublicKey puk]
            (with "audience" puk))
          (field [this field value] (with field value))
          (last [_]
            (with last-by-id true))
          (localTuples [this]
            (rx/map reify-tuple (rx-query-tuples tuple-base criteria false)))
          (tuples [this]
            (rx/observable*
              (fn [^Subscriber subscriber]
                (let [sub {"type" "sub" "author" own-puk "criteria" criteria}]
                  (store-tuple tuple-base (timestamped sub) sub))
                (let [^Observable tuples (rx/map reify-tuple (rx-query-tuples tuple-base criteria true))]
                  (. subscriber add
                    (. tuples subscribe subscriber))))))))))

(defn get-author [criteria]
  (get criteria "author"))

(defn reify-tuple-space [own-puk tuple-base]
  (reify TupleSpace
    (publisher [_]
      (new-tuple-publisher tuple-base {"author" own-puk}))
    (filter [_]
      (new-tuple-filter tuple-base own-puk))))
