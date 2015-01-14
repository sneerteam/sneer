(ns sneer.server.main-new
  (:require [sneer.networking.udp :as udp]
            [sneer.server.router-connector :as connector]
            [clojure.core.async :as async :refer [chan filter< close! alts!! <!! timeout]]
            [sneer.networking.udp :as udp])
  (:gen-class))

(defn update-puk-address! [puk->address [address payload]]
  (let [puk (:from payload)]
    (swap! puk->address assoc puk address))
  payload)

(defn with-address [puk->address payload]
  (let [puk (:to payload)]
    [(get @puk->address puk) (dissoc payload :to)]))

(defn has-address? [[address payload]]
  address)

(defn is-routable? [[address payload]]
  (-> payload :from some?))

(defn trace-changes [label atom]
  (add-watch atom (Object.)
             (fn [_key _ref old-value new-value]
               (when (not= old-value new-value)
                 (println label new-value)))))

(defn start [port]
  (let [queue-size 10
        puk->address (atom {})
        packets-in (chan)
        packets-out (chan)
        routable-packets-in (filter<
                              is-routable?
                              packets-in)
        routable-packets-out (filter<
                               has-address?
                               (async/map #(with-address puk->address %) [packets-out]))
        udp-server (udp/start-udp-server packets-in routable-packets-out port)]

    (connector/start-connector
      queue-size
      (async/map #(update-puk-address! puk->address %) [routable-packets-in])
      packets-out)

    (trace-changes "[PUK->ADDRESS]" puk->address)

    {:udp-server udp-server :packets-in packets-in :packets-out packets-out}))

(defn stop [server]
  (close! (:packets-out server))
  (alts!! [(:udp-server server) (timeout 500)]))

(defn -main [& [port-string]]
  (let [port (when-some [p port-string] (Integer/parseInt p))
        server (start (or port 5555))]
    (println "udp-server finished with" (<!! (:udp-server server)))))
