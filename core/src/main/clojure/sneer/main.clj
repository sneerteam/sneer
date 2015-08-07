(ns sneer.main
  (:require [clojure.core.async :refer [chan >! <! timeout map< map>]]
            [sneer.admin :as admin]
            [sneer.tuple-base-provider :refer :all]
            [sneer.networking.client :as network-client]
            [sneer.networking.udp :as udp]
            [sneer.async :refer [go-trace]]
            [sneer.tuple.transmitter :as transmitter])
  (:import [java.net InetSocketAddress]
           [sneer.commons SystemReport]
           [sneer.admin SneerAdmin]))

(defn- resolved-address!! [host port]
  (let [ret (InetSocketAddress. host port)]
    (if (.isUnresolved ret)
      (do
        (SystemReport/updateReport "network/dns-unresolved" ret)
        (Thread/sleep 4000)          ;; Use some sort of network availability lease instead of just polling.
        (recur host port))
      ret)))

(defn start-client [puk tuple-base & [host port]]
  (let [server-addr (future (resolved-address!! (or host "dynamic.sneer.me") (or port 5555)))
        udp-in (chan)
        udp-out (chan)
        to-me (map< second udp-in)
        to-server (map> #(do [@server-addr %]) udp-out)
        tuples-received (chan)
        client (network-client/start-client puk to-me to-server tuples-received)
        connect-to-follower-fn #(network-client/connect-to-follower client %1 %2)]

    (transmitter/start puk tuple-base tuples-received connect-to-follower-fn)

    (udp/start-udp-server udp-in udp-out)

    ; server ping loop
    (let [ping {:from puk}]
      (go-trace
        (when (>! to-server ping)
          (<! (timeout 200)) ; Second ping right after the first is good for local test runs.
          (while (>! to-server ping)
           (<! (timeout 20000))))))

    udp-out))

(defn start [db]
  (let [^SneerAdmin admin (admin/new-sneer-admin-over-db db)
        tuple-base (tuple-base-of admin)
        own-prik (. admin privateKey)
        puk (. own-prik publicKey)]

    (start-client puk tuple-base)

    (admin/new-sneer-admin own-prik tuple-base)))

(gen-class
  :name "sneer.main.SneerAdminFactoryServiceProvider"
  :implements [sneer.admin.SneerAdminFactory$ServiceProvider]
  :prefix "-service-provider-")

(defn -service-provider-create [_ db]
  (start db))

