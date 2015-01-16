(ns sneer.main
  (:require [clojure.core.async :refer [chan >! <! timeout map< map>]]
            [sneer.admin :as admin]
            [sneer.networking.client :as network-client]
            [sneer.networking.udp :as udp]
            [sneer.async :refer [go-trace]]
            [sneer.tuple.tuple-transmitter :as transmitter])
  (:import (java.net InetSocketAddress)
           (sneer.admin SneerAdmin)))

(defn start-client [puk tuple-base & [host port]]
  (let [server-addr (future (InetSocketAddress. (or host "dynamic.sneer.me") (or port 5555)))
        udp-in (chan)
        udp-out (chan)
        to-me (map< second udp-in)
        to-server (map> #(do [@server-addr %]) udp-out)
        tuples-received (chan)
        client (network-client/start-client puk to-me to-server tuples-received)
        connect-to-follower-fn #(network-client/connect-to-follower client %1 %2)
        ping {:from puk}]

    (transmitter/start puk tuple-base tuples-received connect-to-follower-fn)

    (udp/start-udp-server udp-in udp-out)

    ; server ping loop
    (go-trace
      (while (>! to-server ping)
        (<! (timeout 20000))))

    udp-out))

(defn start [db]
  (let [^SneerAdmin admin (admin/new-sneer-admin-over-db db)
        tuple-base (admin/tuple-base-of admin)
        own-prik (. admin privateKey)
        puk (. own-prik publicKey)]

    (start-client puk tuple-base)

    (admin/new-sneer-admin own-prik tuple-base)))
