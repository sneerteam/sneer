(ns sneer.tuple.tuple-transmitter
  (:require [clojure.core.async :refer [chan go-loop >! <! filter> map>]]
            [sneer.async :refer [go-while-let go-trace sliding-chan dropping-chan]]
            [sneer.commons :refer [produce!]]
            [sneer.tuple.persistent-tuple-base :as ptb :refer [query-tuples store-tuple get-local-attribute set-local-attribute]])
  (:import [sneer.commons SystemReport]))

(defn- visible-to? [puk tuple]
  (let [audience (get tuple "audience")]
    (or (nil? audience) (= puk audience))))

(defn start [own-puk tuple-base tuples-in connect-to-follower-fn]
  (let [peer-chans (atom {})
        chan-for-peer (fn [follower-puk]
                        (let [c (chan)]
                          (connect-to-follower-fn follower-puk c)
                          c))
        produce-chan (partial produce! chan-for-peer peer-chans)]

    (go-while-let [tuple (<! tuples-in)]
      (store-tuple tuple-base tuple))

    (let [subs (chan)
          subs-lease (chan)]
      (query-tuples tuple-base {"type" "sub" "audience" own-puk} subs subs-lease)

      (go-while-let [sub (<! subs)]

        (SystemReport/updateReport "tuples/last-sub" sub)

        (let [sub-id (sub "id")
              follower (sub "author")
              acks (sliding-chan)
              follower-chan (->> (produce-chan follower)
                                 (filter> (fn [tuple] (visible-to? follower tuple)))
                                 (map> (fn [tuple] [tuple acks])))
              sub-lease (chan)
              last-id-sent (chan)]

          (get-local-attribute tuple-base "last-id-sent" 0 sub-id last-id-sent)

          (let [last-id-sent (<! last-id-sent)
                criteria (sub "criteria")]
            (query-tuples tuple-base
                          (assoc criteria ptb/after-id last-id-sent)
                          follower-chan
                          sub-lease))

          (go-while-let [last-tuple-sent (<! acks)]
            (set-local-attribute tuple-base "last-id-sent" (last-tuple-sent "id") sub-id)))))

    (let [subs (chan)
          subs-lease (chan)
          subs-acks (dropping-chan)]

      (query-tuples tuple-base {"type" "sub" "author" own-puk} subs subs-lease)

      (go-while-let [sub (<! subs)]
        (if-some [followee (get-in sub ["criteria" "author"])]
          (when-not (= own-puk followee)
            (let [followee-chan (produce-chan followee)]
              (go-trace (>! followee-chan [(assoc sub "audience" followee) subs-acks]))))
          (println "INVALID SUB! Author missing:" sub))))))
