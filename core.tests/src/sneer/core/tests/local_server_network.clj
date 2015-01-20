(ns sneer.core.tests.local-server-network
  (:require [clojure.core.async :as async]
            [sneer.server.router-connector :as router]
            [sneer.tuple.tuple-transmitter :as transmitter]
            [sneer.networking.client :as network-client]
            [sneer.server.main :as server]
            [sneer.main :as main]
            [sneer.async :refer [go-trace]]
            [sneer.core.tests.network :refer [Network]])
  (:import (java.io Closeable)))

(defn start-local []
  (let [server-port 5454
        server (server/start server-port)
        lease (async/chan)]

    (reify Network
      (connect [network puk tuple-base]
        (println "Network/connect" puk)

        (let [client (main/start-client puk tuple-base "localhost" server-port)]
          (go-trace
            (async/<! lease)
            (async/close! client))))

      Closeable
      (close [network]
        (async/close! lease)
        (server/stop server)))))


(defn start []
  (let [lease (async/chan)]

    (reify Network
      (connect [network puk tuple-base]
        (println "Network/connect" puk)

        (let [client (main/start-client puk tuple-base)]
          (go-trace
            (async/<! lease)
            (async/close! client))))

      Closeable
      (close [network]
        (async/close! lease)))))
