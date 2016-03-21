(ns sneer.view)

(defn- same-id? [id contact]
  (= id (:contact-id contact)))

(defn add-contact [state contact]
  (update-in state [:convo-list] conj contact))

(defn delete-contact [state contact-id]
  (let [same-id? (partial same-id? contact-id)]
    (update-in state [:convo-list] #(remove same-id? %))))

(def initial
  {:convo-list []})

