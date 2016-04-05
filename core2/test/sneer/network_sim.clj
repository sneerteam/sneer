(ns sneer.network-sim)

(defn send-packet [network packet]
  (assert (:from packet))
  (let [addr     (packet :to)
        inbox-fn (@network addr)]
    (inbox-fn {:send (packet :send)})))

(defn join [network addr inbox-fn]
  (swap! network assoc addr inbox-fn))

(defn network-sim []
  (atom nil))
