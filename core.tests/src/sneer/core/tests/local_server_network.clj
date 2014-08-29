(ns sneer.core.tests.local-server-network
  (:require [clojure.core.async :as async]
            [sneer.core :as core]
            [sneer.server.main :as server]
            [sneer.networking.client :as client]))

(defn compromised [ch]
  (async/filter> (fn [_] (> (rand) 0.7)) ch))

(defn compromised-if [unreliable ch]
  (if unreliable
    (compromised ch)
    ch))

(defn start [& [unreliable]]
  (let [port 4242
        server (server/start port)]
    (reify core/Network
      (connect [network puk]
        (let [to-server (compromised-if unreliable (async/chan 1))
              from-server (compromised-if unreliable (async/chan 1))
              connection (client/create-connection puk from-server to-server "localhost" port)]
          connection))
      core/Disposable
      (dispose [network]
        (server/stop server)))))
