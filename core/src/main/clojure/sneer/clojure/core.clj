(ns sneer.clojure.core)

(defn foo! []
  (throw (RuntimeException. "it works!")))
