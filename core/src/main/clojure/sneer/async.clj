(ns sneer.async
  (:require [clojure.core.async :as async :refer [chan]]))

(defn <!!? [ch]
  (async/alt!!
    (async/timeout 200) ::timeout
    ch ([v] v)))
         
(defn >!!? [ch v]
  (async/alt!!
    (async/timeout 200) false
    [[ch v]] true))

(defn dropping-chan [& [n]]
  (chan (async/dropping-buffer (or n 1))))

(defn sliding-chan [& [n]]
  (chan (async/sliding-buffer (or n 1))))

(defmacro go-trace
  [& forms]
  `(async/go
     (try
       ~@forms
       (catch java.lang.Exception ~'e
         (println "GO ERROR" ~'e)
         (. ~'^Exception e printStackTrace)))))
