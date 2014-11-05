(ns sneer.async
  (:require [clojure.core.async :as async :refer [chan go]]))

(def IMMEDIATELY (doto (async/chan) async/close!))

(defn dropping-chan [& [n]]
  (chan (async/dropping-buffer (or n 1))))

(defn sliding-chan [& [n]]
  (chan (async/sliding-buffer (or n 1))))

(defmacro go-trace
  [& forms]
  `(go
     (try
       ~@forms
       (catch java.lang.Exception ~'e
         (println "GO ERROR" ~'e)
         (. ~'^Exception e printStackTrace)))))
