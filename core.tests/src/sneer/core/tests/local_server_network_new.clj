(ns sneer.core.tests.local-server-network-new
  (:require [clojure.core.async :as async]
            [sneer.server.router-connector :as router]
            [sneer.tuple.tuple-transmitter :as transmitter]
            [sneer.networking.client-new :as network-client]
            [sneer.async :refer [go-while-let]]))

(defprotocol Network
  (connect
    [network puk tuple-base]
    "Connects tuple-base to the network."))

(defn- tap-for [mult]
  (let [c (async/chan)]
    (async/tap mult c)
    c))

(defn start [& [unreliable]]
  (let [queue-size 2
        to-server (async/chan)
        to-server-mult (async/mult to-server)
        to-server-tap (tap-for to-server-mult)
        to-clients (async/chan)
        to-clients-mult (async/mult to-clients)
        resend-timeout-fn #(async/timeout 50)
        server (router/start-connector queue-size to-server-tap to-clients resend-timeout-fn)]
    
    (let [debug-client (tap-for to-clients-mult)
          debug-server (tap-for to-server-mult)
          debug (async/merge [debug-client debug-server])]
      (go-while-let [packet (async/<! debug)]
        (println "PACKET" packet)))

    (reify Network
      (connect [network puk tuple-base]
        (println "connect" network puk tuple-base)

        (let [to-clients-tap (tap-for to-clients-mult)
              to-me (async/map< #(dissoc % :to) (async/filter< #(= (:to %) puk) to-clients-tap))
              tuples-received (async/chan)
              client (network-client/start-client puk to-me to-server tuples-received)
              connect-to-follower-fn #(network-client/connect-to-follower client %1 %2)]

          ;;(async/>!! to-server {:from puk})
          (transmitter/start puk tuple-base tuples-received connect-to-follower-fn)

          ))

      java.io.Closeable
      (close [network]
        (async/close! to-server)
        (async/close! to-clients)))))
