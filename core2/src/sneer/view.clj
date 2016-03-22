(ns sneer.view
  (:require [sneer.util :as util]))

(defn- same-id? [id contact]
  (= id (:contact-id contact)))

(defn add-contact [state contact]
  (update-in state [:convo-list] conj contact))

(defn delete-contact [state contact-id]
  (let [same-id? (partial same-id? contact-id)]
    (update-in state [:convo-list] #(into [] (remove same-id? %)))))

(defn rename-contact [state contact-id new-nick]
  (update-in state [:convo-list] #(into [] (util/update-in %
                                                           [:contact-id contact-id]
                                                           [:nick new-nick]))))

(def initial
  {:convo-list []})

