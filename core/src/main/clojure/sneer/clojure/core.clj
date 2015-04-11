(ns sneer.clojure.core)

(defn nvl [v default]
  (if (some? v) v default))

(defn foo! []
  (throw (RuntimeException. "it works!")))
