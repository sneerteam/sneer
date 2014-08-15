(ns sneer.core.tests.local-server
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :as async]
            [sneer.core :as core]
            [sneer.server.main :as server]
            [sneer.networking.client :as client]
            [sneer.rx :refer [subject*]]))

(defn chan->observable [ch]
  (rx.Observable/empty))

(defn chan->observer [ch]
  (reify rx.Observer
    (onNext [this value])
    (onError [this error])
    (onCompleted [this])))

(defn start []
  (let [port 4242
        server (server/start port)]
    (reify core/Network
      (connect [network puk]
        (let [to-server (async/chan 1)
              from-server (async/chan 1)
              connection (client/start puk from-server to-server "localhost" port)]
          (subject* (chan->observable from-server)
                    (chan->observer to-server))))
      (dispose [network]
        (server/stop server)))))
