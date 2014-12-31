(ns sneer.server.main
  (:require [sneer.networking.udp-old :as udp]
            [sneer.server.router-old :as router]
            [clojure.core.async :as async :refer [chan filter< close! alts!! <!! timeout]])
  (:gen-class))

#_(def home-dir (System/getProperty "user.home"))

#_(defn storage-dir [args]
   (if (nil? args)
     home-dir
     (first args)))

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
  (let [puk->address (atom {})
        packets-in (chan)
        packets-out (chan)
        routable-packets-in (filter<
                             is-routable?
                             packets-in)
        routable-packets-out (filter<
                              has-address?
                              (async/map #(with-address puk->address %) [packets-out]))
        udp-server (udp/serve-udp packets-in routable-packets-out port)]

    (router/start
      (async/map #(update-puk-address! puk->address %) [routable-packets-in])
      packets-out)

    (trace-changes "[PUK->ADDRESS]" puk->address)

    {:udp-server udp-server :packets-in packets-in :packets-out packets-out}))

(defn stop [server]
  (close! (:packets-out server))
  (alts!! [(:udp-server server) (timeout 500)]))

(defn -main [& [port]]
  (let [server (start (or (Integer/parseInt port) 4242))]
    (println "udp-server finished with" (<!! (:udp-server server)))))
