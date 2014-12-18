(ns sneer.tuple.tuple-transmitter
  (:require [clojure.core.async :refer [chan go-loop <!]]
            [sneer.async :refer [go-while-let]]
            [sneer.commons :refer [produce!]]
            [sneer.tuple.persistent-tuple-base :refer [query-tuples store-tuple]]))

(defn- start-sub [tuple-base sub follower-chan]
  (let [sub-lease (chan)]
    (query-tuples tuple-base (sub "criteria") follower-chan sub-lease)
    sub-lease))

(defn start [own-puk tuple-base tuples-in connect-to-follower-fn]
  (let [subs (chan)
        subs-lease (chan)
        follower-chans (atom {})
        chan-for-folower (fn [follower-puk]
                           (let [c (chan)]
                             (connect-to-follower-fn follower-puk c)
                             c))]
    (query-tuples tuple-base {"type" "sub" "audience" own-puk} subs subs-lease)

    (go-while-let [sub (<! subs)]
      (let [follower (sub "author")
            follower-chan (produce! chan-for-folower follower-chans follower)]
        (start-sub tuple-base sub follower-chan)))

    (go-while-let [tuple (<! tuples-in)]
      (store-tuple tuple-base tuple))))
