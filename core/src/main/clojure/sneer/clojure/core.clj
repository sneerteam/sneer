(ns sneer.clojure.core)

(defn nvl [v default]
  (if (some? v) v default))

