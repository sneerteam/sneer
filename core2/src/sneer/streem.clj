(ns sneer.streem
  (:require
    [sneer.util :refer [conj-vec]]))

(defn catch-up!
  ([streems f]
   (catch-up! streems f nil))
  ([streems f initial-value]
   (catch-up! streems f initial-value ::all-events))
  ([streems f initial-value streem-id]
   (reduce f initial-value (@streems streem-id))))

(defn- append-with-id [state event streem-id]
  (let [all-events (state ::all-events)
        event (assoc event :id (count all-events))]
    (cond-> (assoc state ::all-events (conj-vec all-events event))
      streem-id
      (update-in [streem-id] conj-vec event))))

(defn append! [streems event streem-id]
  (swap! streems append-with-id event streem-id))

(defn streems []
  (atom {}))