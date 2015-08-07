(ns sneer.tuple.transmitter
  (:require [clojure.core.async :refer [chan go-loop >! <! filter> map>]]
            [sneer.async :refer [go-while-let go-trace sliding-chan dropping-chan]]
            [sneer.commons :refer [produce!]]
            [sneer.tuple.persistent-tuple-base :as ptb]
            [sneer.tuple.protocols :refer [query-tuples store-tuple get-local-attribute set-local-attribute]])
  (:import [sneer.commons SystemReport]))

(defn- visible-to? [puk tuple]
  (let [audience (get tuple "audience")]
    (or (nil? audience) (= puk audience))))

(defn normalize-audience-for-sub [tuple]
  (assoc tuple "audience" (get-in tuple ["criteria" "author"])))

(defn- set-last-ids-sent [acks tuple-base sub-id]
  (go-while-let [last-tuple-sent (<! acks)]
    (set-local-attribute tuple-base "last-id-sent" (last-tuple-sent "id") sub-id)))

(defn- handle-subs [tuple-base produce-chan subs]
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

          (set-last-ids-sent acks tuple-base sub-id))))

(defn- do-send-acks [followee-chan tuple send-acks]
  (go-trace (>! followee-chan [tuple send-acks])))

(defn- get-sent [send send-acks own-puk tuple-base sent? produce-chan]
  (go-while-let [tuple (<! send)]
        (if-some [followee (get tuple "audience")]
          (when-not (= own-puk followee)
            (get-local-attribute tuple-base "sent?" false (tuple "id") sent?)
            (when-not (<! sent?)
              (let [followee-chan (produce-chan followee)]
                (do-send-acks followee-chan tuple send-acks))))
          (println "INVALID SUB! Audience missing:" tuple))))

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

      (handle-subs tuple-base produce-chan subs))

    (let [send (chan)
          send-lease (chan)
          send-acks (chan)
          sent? (chan)]

      (query-tuples tuple-base
                    {"type" "sub" "author" own-puk}
                    (map> normalize-audience-for-sub send)
                    send-lease)
      (query-tuples tuple-base {"type" "push" "author" own-puk} send send-lease)

      (go-while-let [ack (<! send-acks)]
        (set-local-attribute tuple-base "sent?" true (ack "id")))

      (get-sent send send-acks own-puk tuple-base sent? produce-chan))))
