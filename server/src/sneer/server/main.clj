(ns sneer.server.main
  (:require [sneer.networking.udp :as udp]
            [sneer.server.router-connector :as connector]
            [sneer.server.http-server :as http-server]
            [sneer.async :refer [go-trace go-while-let]]
            [clojure.core.async :as async :refer [chan filter< close! alts!! <! <!! timeout]]
            [sneer.networking.udp :as udp])
  (:import [java.io File])
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

(defn- trace-in [c label]
  (async/map< #(do (println label %) %) c))

(defn start [udp-port http-port prevalence-dir]
  (let [connector-prevalence-file (File. prevalence-dir "server.jr")
        gcm-prevalence-file (File. prevalence-dir "gcm.jr")
        puk->address (atom {})
        packets-in (chan)
        packets-out (chan)
        routable-packets-in (filter<
                              is-routable?
                              packets-in)
        routable-packets-in (trace-in routable-packets-in "IN ")
        routable-packets-out (filter<
                               has-address?
                               (async/map #(with-address puk->address %) [packets-out]))
        routable-packets-out (trace-in routable-packets-out "OUT")
        udp-server (udp/start-udp-server packets-in routable-packets-out udp-port)
        puks-to-notify (async/chan)
        http-server (when http-port
                      (http-server/start gcm-prevalence-file http-port puks-to-notify))]

    (go-trace
      (<! (connector/start-connector
            connector-prevalence-file
            (async/map #(update-puk-address! puk->address %) [routable-packets-in])
            packets-out
            puks-to-notify))
      (close! puks-to-notify))

    (trace-changes "[PUK->ADDRESS]" puk->address)

    {:udp-server udp-server :http-server http-server :packets-in packets-in :packets-out packets-out}))

(defn stop [server]
  (close! (:packets-in server))
  (let [udp-server  (:udp-server  server)
        http-server (:http-server server)
        timeout (timeout 2000)]

    (alts!! [udp-server timeout])
    (when http-server
      (alts!! [http-server timeout]))))

(defn- optional-int [string default-value]
  (if (some? string)
    (Integer/parseInt string)
    default-value))

(defn -main [& [udp-port http-port]]
  (let [server (start (optional-int udp-port 5555)
                      (optional-int http-port 80)
                      (File. "."))]
    (println "udp-server finished with" (<!! (:udp-server server)))))
