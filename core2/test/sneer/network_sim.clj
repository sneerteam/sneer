(ns sneer.network-sim)

(defn send-packet [network packet]
  (let [addr     (packet :to)
        inbox-fn (@network addr)]
    (inbox-fn (dissoc packet :to))))

(defn join [network addr inbox-fn]
  (swap! network assoc addr inbox-fn))

(defn network-sim []
  (atom nil))