(ns sneer.main
  (:require [clojure.core.async :refer [chan >! <! timeout map< map>]]
            [sneer.admin-new :as admin]
            [sneer.networking.client-new :as network-client]
            [sneer.networking.udp :as udp]
            [sneer.async :refer [go-trace]]
            [sneer.tuple.tuple-transmitter :as transmitter])
  (:import (java.net InetSocketAddress)
           (sneer.admin SneerAdmin)))

(defn start [db]
  (let [^SneerAdmin admin (admin/new-sneer-admin-over-db db)
        tuple-base (admin/tuple-base-of admin)
        own-prik (. admin privateKey)
        puk (. own-prik publicKey)
        admin (admin/new-sneer-admin own-prik tuple-base)
        server-addr (InetSocketAddress. "dynamic.sneer.me" 5555)
        udp-in (chan)
        udp-out (chan)
        to-me (map< second udp-in)
        to-server (map> #(do [server-addr %]) udp-out)
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

    admin))