(ns sneer.core.tests.local-server-network
  (:require [clojure.core.async :as async]
            [sneer.core :as core]
            [sneer.server.main :as server]
            [sneer.networking.client :as client]))

(defn start [& [unreliable]]
  (let [port 4242
        server (server/start port)]
    (reify core/Network
      (connect [network puk]
        (let [to-server (async/chan 1)
              from-server (async/chan 1)
              connection (client/create-connection puk from-server to-server "localhost" port unreliable)]
          connection))
      core/Disposable
      (dispose [network]
        (server/stop server)))))
