(ns sneer.core.tests.local-server-network-new
  (:require [clojure.core.async :as async]
            [sneer.server.router-connector :as router]
            [sneer.tuple.tuple-transmitter :as transmitter]
            [sneer.networking.client-new :as network-client]))

(defprotocol Network
  (connect
    [network puk tuple-base]
    "Connects tuple-base to the network."))

(defn start [& [unreliable]]
  (let [queue-size 2
        to-server (async/chan)
        to-clients (async/chan)
        to-clients-mult (async/mult to-clients)
        resend-timeout-fn #(async/timeout 100)
        server (router/start-connector queue-size to-server to-clients resend-timeout-fn)]
    (reify Network
      (connect [network puk tuple-base]
        (let [to-clients-tap (doto (async/chan) #(async/tap to-clients-mult %))
              to-me (async/map< #(dissoc % :to) (async/filter< #(= (:to %) puk) to-clients-tap))
              tuples-received (async/chan)
              client (network-client/start-client puk to-me to-server tuples-received)
              connect-to-follower-fn (partial network-client/connect-to-follower client)]
          (transmitter/start puk tuple-base tuples-received connect-to-follower-fn)))

      java.io.Closeable
      (close [network]
        (async/close! to-server)
        (async/close! to-clients)))))
