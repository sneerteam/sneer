(ns sneer.core.tests.local-server-network-new
  (:require [clojure.core.async :as async]
            [sneer.server.router-connector :as router]
            [sneer.tuple.tuple-transmitter :as transmitter]
            [sneer.networking.client-new :as network-client]
            [sneer.server.main-new :as server]
            [sneer.main :as main]
            [sneer.async :refer [go-trace]])
  (:import (java.io Closeable)))

(defprotocol Network
  (connect
    [network puk tuple-base]
    "Connects tuple-base to the network."))

(defn start [& [unreliable]]
  (let [server-port 5454
        server (server/start server-port)
        close (async/chan)]

    (reify Network
      (connect [network puk tuple-base]
        (println "Network/connect" puk)

        (let [client (main/start-client puk tuple-base "localhost" server-port)]
          (go-trace
            (async/<! close)
            (async/close! client))))

      Closeable
      (close [network]
        (async/close! close)
        (server/stop server)))))
