(ns sneer.tuple.tuple-transmitter
  (:require [clojure.core.async :refer [chan go-loop >! <!]]
            [sneer.async :refer [go-while-let go-trace]]
            [sneer.commons :refer [produce!]]
            [sneer.tuple.persistent-tuple-base :refer [query-tuples store-tuple]]))

(defn start [own-puk tuple-base tuples-in connect-to-follower-fn]
  (let [follower-chans (atom {})
        chan-for-follower (fn [follower-puk]
                            (let [c (chan)]
                              (connect-to-follower-fn follower-puk c)
                              c))]
    (go-while-let [tuple (<! tuples-in)]
      (store-tuple tuple-base tuple))
    
    (let [subs (chan)
          subs-lease (chan)]
      (query-tuples tuple-base {"type" "sub" "audience" own-puk} subs subs-lease)

      (go-while-let [sub (<! subs)]
        (let [follower (sub "author")
              follower-chan (produce! chan-for-follower follower-chans follower)
              sub-lease (chan)]
          (query-tuples tuple-base (sub "criteria") follower-chan sub-lease))))

    (let [subs (chan)
          subs-lease (chan)]
      (query-tuples tuple-base {"type" "sub" "author" own-puk} subs subs-lease)

      (go-while-let [sub (<! subs)]
        (if-some [follower (sub "audience")]
          (let [follower-chan (produce! chan-for-follower follower-chans follower)]
            (go-trace (>! follower-chan sub)))
          (println "INVALID SUB! Audience missing:" sub))))))
