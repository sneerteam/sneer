(ns sneer.core.tests.local-server
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :as async :refer [<! >!!]]
            [sneer.core :as core]
            [sneer.server.main :as server]
            [sneer.networking.client :as client]
            [sneer.rx :refer [subject*]]))

(defn chan->observable [ch]
  (let [subject (rx.subjects.PublishSubject/create)]
    (async/go-loop []
      (when-let [value (<! ch)]
        (rx/on-next subject value)
        (recur)))
    subject))

(defn chan->observer [ch]
  (reify rx.Observer
    (onNext [this value]
      (>!! ch value))
    (onError [this error]
      (.printStackTrace error)
      (async/close! ch))
    (onCompleted [this]
      (async/close! ch))))

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
