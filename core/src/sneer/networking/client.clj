(ns sneer.networking.client
  (:require [sneer.networking.udp :as udp]
            [clojure.core.async :as async :refer [<! >!]])
  (:import [java.net InetSocketAddress]
           [sneer.commons SystemReport]))

(defn dropping-chan [size]
  (async/chan (async/dropping-buffer size)))

(defn start

  ([puk]
    (start puk (dropping-chan 1) (dropping-chan 1)  "dynamic.sneer.me" 5555))

  ([puk from-server to-server server-host server-port]
    (let [packets-in (async/chan)
          packets-out (async/chan)]

      (async/thread

        ; ensure no network activity takes place on caller thread to workaround android limitation
        (let [server-addr (InetSocketAddress. server-host server-port)
              udp-server (udp/serve-udp packets-in packets-out)
              ping [server-addr {:intent :ping :from puk}]]

          ; server ping loop
          (async/go-loop []
            (when  (>! packets-out ping)
              (<! (async/timeout 20000))
              (recur)))

          ; just report received packets for now
          (async/go-loop []
            (when-let [packet (<! packets-in)]
              (SystemReport/updateReport "packet" packet)
              (when-let [payload (-> packet second :payload)]
                (>! from-server payload))
              (recur)))

          (async/pipe
           (async/map
            (fn [payload] [server-addr {:intent :send :from puk :to (:address payload) :payload payload}])
            [to-server])
           packets-out)))

      {:packets-in packets-in :packets-out packets-out :from-server from-server :to-server to-server})))

(defn stop [client]
  (async/close! (:packets-out client)))
