(ns sneer.contact
  (:require [sneer.util :as util]))

(defn- same-id? [id contact]
  (= id (:contact-id contact)))

(defn add [state contact]
  (let [state (or state [])]
    (conj state contact)))

(defn delete [state contact-id]
  (let [same-id? (partial same-id? contact-id)]
    (into [] (remove same-id? state))))

(defn rename [state contact-id new-nick]
  (util/update-where state [:contact-id contact-id] [:nick new-nick]))

(defn contact [event]
  (select-keys event [:nick]))
