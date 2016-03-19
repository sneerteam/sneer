(ns sneer.view)

(defn add-contact [state contact]
  (update-in state [:convo-list] conj contact))

(def initial
  {:convo-list []})