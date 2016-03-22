(ns sneer.contact)

(defn- same-id? [id contact]
  (= id (:contact-id contact)))

(defn add [state contact]
  (let [state (or state [])]
    (conj state contact)))

(defn delete [state contact-id]
  (let [same-id? (partial same-id? contact-id)]
    (into [] (remove same-id? state))))
    (into [] (remove same-id? state))))

(defn contact [event]
  (select-keys event [:nick]))
