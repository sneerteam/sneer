(ns sneer.persistent-tuple-base
  (:require [sneer.core :as core]))

(defn create []
  (reify core/TupleBase
    (store-tuple [this tuple])
    (query-tuples [this criteria keep-alive]
      (rx.Observable/empty))))