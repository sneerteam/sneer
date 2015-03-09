(ns sneer.restartable)

(defprotocol Restartable
  (restart [_]))
