(ns sneer.rx-macros
  (:require [rx.lang.clojure.interop :as rx-interop]))

(defmacro rx-defer [& body]
  `(rx.Observable/defer
     (rx-interop/fn [] ~@body)))
