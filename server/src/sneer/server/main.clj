(ns sneer.server.main
  (:require [sneer.server.udp :as udp])
  (:gen-class))

(def home-dir (System/getProperty "user.home"))

(defn storage-dir [args]
  (if (nil? args)
    home-dir
    (first args)))

(defn -main [& args]
  (udp/serve-udp))
