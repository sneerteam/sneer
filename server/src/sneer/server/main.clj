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

(defn start-server! [port]
  (let [packets-in (async/chan)
        packets-out (async/chan)
        udp-server (udp/serve-udp
                    port
                    packets-in
                    (async/map (fn [packet] [(:to packet) (dissoc packet :to)]) [packets-out]))
        router (router/create-router
                (async/map (fn [[address packet]] (assoc packet :from address)) [packets-in])
                 packets-out)]
    {:udp-server udp-server :packets-in packets-in :packets-out packets-out}))

(defn -main [& [port]]
  (let [server (start-server! (or (Integer/parseInt port) 4242))]
    (println "udp-server finished with" (async/<!! (:udp-server server)))))
