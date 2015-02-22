(ns sneer.core.tests.local-server-network
  (:require [clojure.core.async :as async]
            [sneer.server.main :as server]
            [sneer.main :as main]
            [sneer.async :refer [go-trace]]
            [sneer.core.tests.network :refer [Network]])
  (:import (java.io Closeable File)))


(defn- create-temp-dir [prefix]
  (let [temp (File/createTempFile prefix ".tmp")]
    (assert (.delete temp))
    (assert (.mkdir temp))
    temp))

(defn start-local []
  (let [udp-port 5454
        some-random-number 9876
        http-port some-random-number
        server (server/start udp-port http-port (create-temp-dir "local-server-network"))
        lease (async/chan)]

    (reify Network
      (connect [_ puk tuple-base]
        (println "Network/connect" puk)

        (let [client (main/start-client puk tuple-base "localhost" udp-port)]
          (go-trace
            (async/<! lease)
            (async/close! client))))

      Closeable
      (close [_]
        (async/close! lease)
        (server/stop server)))))


(defn start []
  (let [lease (async/chan)]

    (reify Network
      (connect [_ puk tuple-base]
        (println "Network/connect" puk)

        (let [client (main/start-client puk tuple-base)]
          (go-trace
            (async/<! lease)
            (async/close! client))))

      Closeable
      (close [_]
        (async/close! lease)))))
