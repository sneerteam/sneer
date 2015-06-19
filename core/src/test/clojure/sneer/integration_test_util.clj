(ns sneer.integration-test-util
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [close! chan <! >!]]
            [sneer.async :refer [go-while-let]]
            [sneer.test-util :refer [emits]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.tuple-transmitter :as transmitter])
  (:import [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]
           [java.io Closeable]
           [sneer.flux LeaseHolder]
           [sneer.tuple.protocols Database]))

(defn own-puk [admin]
  (.. admin privateKey publicKey))

(defn connect! [admin1 admin2]
  (let [admin1-received (chan)
        admin2-received (chan)
        admin1-puk (own-puk admin1)
        admin2-puk (own-puk admin2)
        admin1-tb (tuple-base-of admin1)
        admin2-tb (tuple-base-of admin2)]

    (transmitter/start admin1-puk admin1-tb admin1-received
                       (fn [follower-puk tuples-out & _]
                         (assert (= follower-puk admin2-puk))
                         (go-while-let [[tuple ack-ch] (<! tuples-out)]
                           (>! admin2-received tuple)
                           (>! ack-ch tuple))))

    (transmitter/start admin2-puk admin2-tb admin2-received
                       (fn [follower-puk tuples-out & _]
                         (assert (= follower-puk admin1-puk))
                         (go-while-let [[tuple ack-ch] (<! tuples-out)]
                           (>! admin1-received tuple)
                           (>! ack-ch tuple))))))

(defn sneer! []
  (let [delegate (Container. (CoreLoader.))
        transient nil]
    (.inject delegate PersistenceFolder (reify PersistenceFolder (get [_] transient)))
    (.inject delegate Database (create-sqlite-db))

    (reify
      IFn
      (invoke [_ component] (.produce delegate component))

      Closeable
      (close [_] (close! (.getLeaseChannel (.produce delegate LeaseHolder)))))))
