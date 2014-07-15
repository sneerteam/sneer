(ns sneer.server.main
  (:require [sneer.server.networking :as networking])
  (:gen-class))

(def home-dir (System/getProperty "user.home"))

(defn sleep []
  (loop []
    (Thread/sleep (* 1000 60))
    (recur)))

(defn storage-dir [args]
  (if (nil? args)
    home-dir
    (first args)))

(defn -main [& args]
  (networking/start-server! (java.io.File. (storage-dir args) ".cloud"))
  (networking/connect-test-client!)
  (sleep))
