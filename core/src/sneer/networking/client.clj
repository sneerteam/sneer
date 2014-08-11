(ns sneer.networking.client
  (:require [sneer.networking.udp :as udp]
            [clojure.core.async :as async :refer [<! >!]])
  (:import [java.net InetSocketAddress]
           [sneer.commons SystemReport]))

(defn start [& [server-host server-port]]

  (let [server-addr (InetSocketAddress. (or server-host "dynamic.sneer.me") (or server-port 5555))
        packets-in (async/chan)
        packets-out (async/chan)
        udp-server (udp/serve-udp packets-in packets-out)
        ping [server-addr {:intent :ping}]]

    ; server ping loop
    (async/go-loop []
      (when  (>! packets-out ping)
        (<! (async/timeout 20000))
        (recur)))

    ; just report received packets for now
    (async/go-loop []
      (when-let [packet (<! packets-in)]
        (SystemReport/updateReport "packet" packet)
        (recur)))

    {:packets-out packets-out}))

(defn stop [client]
  (async/close! (:packets-out client)))
