(ns sneer.async
  (:require [clojure.core.async :as async]))

(defmacro go!
  [& forms]
  `(async/go
     (try
       ~@forms
       (catch java.lang.Exception ~'e
         (println "GO ERROR" ~'e)
         (. ~'e printStackTrace)))))
