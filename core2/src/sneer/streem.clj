(ns sneer.streem)

(defn catch-up!
  ([streems f]
   (catch-up! streems f nil))
  ([streems f initial-value]
   (catch-up! streems f initial-value ::all-events))
  ([streems f initial-value streem-id]
   (reduce f initial-value (@streems streem-id))))

(defn- conj-with-id [events event]
  (let [event-with-id (assoc event :id (count events))]
    (conj events event-with-id)))

(defn- append-with-id [state event streem-id] 
  (update-in state [::all-events] conj-with-id event))

(defn append! [streems event streem-id]
  (swap! streems append-with-id event streem-id))

(defn streems []
  (atom {::all-events []}))