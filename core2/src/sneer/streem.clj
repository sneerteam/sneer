(ns sneer.streem)

(defn catch-up! [streems f]
  (reduce f nil (@streems :all)))

(defn- conj-with-id [events event]
  (let [event-with-id (assoc event :id (count events))]
    (conj events event-with-id)))

(defn append! [streems event streem-id]
  (swap! streems update-in [:all] conj-with-id event))

(defn streems []
  (atom {:all []}))