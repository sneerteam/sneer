(ns sneer.server.main
  (:require [sneer.networking.udp :as udp]
            [sneer.server.router :as router]
            [clojure.core.async :as async])
  (:gen-class))

#_(def home-dir (System/getProperty "user.home"))

#_(defn storage-dir [args]
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

(defn is-routable? [[address payload]]
  (-> payload :from some?))

(defn trace-changes [label atom]
  (add-watch atom nil
             (fn [_key _ref old-value new-value]
               (when (not= old-value new-value)
                 (println label new-value)))))

(defn start [port]
  (let [puk->address (atom {})
        packets-in (async/chan)
        packets-out (async/chan)
        routable-packets-in (async/filter<
                             is-routable?
                             packets-in)
        routable-packets-out (async/filter<
                              has-address?
                              (async/map #(with-address puk->address %) [packets-out]))
        udp-server (udp/serve-udp packets-in routable-packets-out port)]

    (router/start
     (async/map #(update-puk-address puk->address %) [routable-packets-in])
     packets-out)

    (trace-changes "[PUK->ADDRESS]" puk->address)

    {:udp-server udp-server :packets-in packets-in :packets-out packets-out}))

(defn stop [server]
  (async/close! (:packets-out server))
  (async/alts!! [(:udp-server server) (async/timeout 500)]))

(defn -main [& [port]]
  (let [server (start (or (Integer/parseInt port) 4242))]
    (println "udp-server finished with" (async/<!! (:udp-server server)))))
