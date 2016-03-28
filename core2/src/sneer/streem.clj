(ns sneer.streem)

(defn catch-up! [streems f]
  (reduce f nil @streems))

(defn- conj-with-id [events event]
  (let [event-with-id (assoc event :id (count events))]
    (conj events event-with-id)))

(defn append [streems event]
  (swap! streems conj-with-id event))

(defn streems []
  (atom []))