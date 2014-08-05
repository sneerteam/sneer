(ns sneer.server.main
  (:require [sneer.server.networking :as networking])
  (:gen-class))

(def home-dir (System/getProperty "user.home"))

(defn storage-dir [args]
  (if (nil? args)
    home-dir
    (first args)))

(defn -main [& args]
  (println "this could be the start of a beautiful program"))
