(ns sneer.networking.client
  (:require [clojure.core.async :as async :refer [<! >! >!! <!!]]
            [rx.lang.clojure.core :as rx]
            [sneer.core :as core]
            [sneer.rx :refer [subject*]]
            [sneer.networking.udp :as udp])
  (:import [java.net InetSocketAddress]
           [sneer.commons SystemReport]))

(defn dropping-chan [size]
  (async/chan (async/dropping-buffer size)))

(defn chan->observable [ch]
  (let [subject (rx.subjects.PublishSubject/create)]
    (async/thread
      (loop []
        (if-let [value (<!! ch)]
          (do
            (rx/on-next subject value)
            (recur))
          (rx/on-completed subject))))
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

(defn start
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
             (when (>! packets-out ping)
               (<! (async/timeout 20000))
               (recur)))

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

(defn create-connection [puk from-server to-server host port]
  (start puk from-server to-server host port)
  (subject* (chan->observable from-server)
            (chan->observer to-server)))

(defn create-network []
  (let [open-channels (atom [])]
    (reify
      core/Network
      (connect [network puk]
        (let [to-server (dropping-chan 1)
              from-server (dropping-chan 1)
              connection (create-connection puk from-server to-server "dynamic.sneer.me" 5555)]
          (swap! open-channels conj to-server)
          connection))
      core/Disposable
      (dispose [network]
        (doall (map async/close! @open-channels))))))
