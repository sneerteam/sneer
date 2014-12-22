(ns sneer.tuple.space
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.serialization :refer [roundtrip]]
   [sneer.commons :refer [now reify+ while-let]]
   [clojure.core.async :refer [thread chan <!! close!]]
   [sneer.tuple.persistent-tuple-base :refer [store-tuple query-tuples]])
  (:import
   [sneer PrivateKey PublicKey]
   [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]))

(defmacro tuple-getter [g]
  `(~g [~'this]
       (get ~'tuple ~(name g))))

(defn reify-tuple [tuple]
  (.toString (get tuple "timestampCreated"))
  (reify+ Tuple
    (get [this key] (get tuple key))
    (tuple-getter type)
    (tuple-getter audience)
    (tuple-getter author)
    (tuple-getter payload)
    (tuple-getter timestampCreated)
    (toString [this] (str tuple))))

(defmacro with-field [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))

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
        (let [tuple (roundtrip (assoc proto-tuple "timestampCreated" (now)))]
          (store-tuple tuples-out tuple)
          (rx/return
            (reify-tuple tuple)))))))

(defn- set-thread-name! [name]
  (.setName (Thread/currentThread) name))

(defn rx-query-tuples [tuple-base criteria keep-alive]
  (rx/observable*
   (fn [^rx.Subscriber subscriber]
     (let [result (chan)]
       (if keep-alive
         (let [lease (chan)]
           (query-tuples tuple-base criteria result lease)
           (.add subscriber (rx/subscription #(do (close! lease) (close! result)))))
         (query-tuples tuple-base criteria result))
       (thread
         (set-thread-name! (str "tuple-query: " criteria))
         (while-let [tuple (<!! result)]
           (rx/on-next subscriber tuple))
         (rx/on-completed subscriber))))))

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
          (localTuples [this]
            (rx-query-tuples tuple-base criteria false))
          (tuples [this]
            (rx/observable*
              (fn [^rx.Subscriber subscriber]
                (store-tuple tuple-base {"type" "sub" "author" own-puk "criteria" criteria})
                (let [^rx.Observable tuples (rx-query-tuples tuple-base criteria true)]
                  (. subscriber add
                    (. tuples subscribe subscriber))))))))))

(defn get-author [criteria]
  (get criteria "author"))

(defn reify-tuple-space [own-puk tuple-base]
  (reify TupleSpace
    (publisher [this]
      (new-tuple-publisher tuple-base {"author" own-puk}))
    (filter [this]
      (new-tuple-filter tuple-base own-puk))))
