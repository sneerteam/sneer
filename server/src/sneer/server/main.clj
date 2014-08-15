(ns sneer.server.main
  (:require [sneer.networking.udp :as udp]
            [sneer.server.router :as router]
            [clojure.core.async :as async])
  (:gen-class))

(def home-dir (System/getProperty "user.home"))

(defn storage-dir [args]
  (if (nil? args)
    home-dir
    (first args)))

(defn update-puk-address [puk->address [address payload]]
  (let [puk (:from payload)]
    (swap! puk->address assoc puk address))
  payload)

(defn with-address [puk->address payload]
  (let [puk (:to payload)]
    [(get @puk->address puk) (dissoc payload :to)]))

(defn has-address? [[address payload]]
  address)

(defn start [port]
  (let [puk->address (atom {})
        packets-in (async/chan)
        packets-out (async/chan)
        udp-server (udp/serve-udp
                    packets-in
                    (async/filter< has-address?
                     (async/map #(with-address puk->address %) [packets-out]))
                    port)
        router (router/create-router
                (async/map #(update-puk-address puk->address %) [packets-in])
                packets-out)]
    {:udp-server udp-server :packets-in packets-in :packets-out packets-out}))

(defn stop [server]
  (async/close! (:packets-out server))
  (async/alts!! [(:udp-server server) (async/timeout 500)]))

(defn -main [& [port]]
  (let [server (start (or (Integer/parseInt port) 4242))]
    (println "udp-server finished with" (async/<!! (:udp-server server)))))
