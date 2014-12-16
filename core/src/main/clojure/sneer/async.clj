(ns sneer.async
  (:require [clojure.core.async :as async :refer [chan go remove< >! <!]]
            [sneer.commons :refer :all]))

(def IMMEDIATELY (doto (async/chan) async/close!))

(defn dropping-chan [& [n]]
  (chan (async/dropping-buffer (or n 1))))

(defn sliding-chan [& [n]]
  (chan (async/sliding-buffer (or n 1))))

(defn connection [in out]
  [in out])
(defn in [connection]
  (first connection))
(defn out [connection]
  (second connection))
(defn other-side [[in out]]
  [out in])

(defmacro go-trace
  [& forms]
  `(go
     (try
       ~@forms
       (catch java.lang.Throwable ~'e
         (println "GO ERROR" ~'e)
         (. ~'^Throwable e printStackTrace)))))

(defmacro go-while-let
  "Makes it easy to continue processing data from a channel until it closes"
  [binding & forms]
  `(go-trace
     (while-let ~binding
                ~@forms)))

(defn distinct-until-changed< [ch]
  (let [ret (chan)]
    (go-trace
      (loop [previous nil]
        (when-some [v (<! ch)]
          (when-not (= v previous)
            (>! ret v))
          (recur v))))
    ret))
