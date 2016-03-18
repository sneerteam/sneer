(ns sneer.contact)

(defn add [state contact]
  (let [state (or state [])]
    (conj state contact)))

(defn contact [event]
  (select-keys event [:nick]))