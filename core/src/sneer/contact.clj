(ns sneer.contact
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [atom->observable]]
   [sneer.party :refer [name-subject produce-party!]])
  (:import
   [sneer Contact]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace]))

(defn duplicate-contact? [nickname party ^Contact contact]
              (or (identical? party (.party contact))
                  (= nickname (.. contact nickname current))))

(defn reify-contact [nickname party]
  (let [nick-subject (ObservedSubject/create nickname)]
    (.subscribe (.observable nick-subject) (name-subject party))
    (reify Contact
      (party [this] party)
      (nickname [this]
        (.observed nick-subject))
      (setNickname [this new-nick]
        (rx/on-next nick-subject new-nick))
      (toString [this]
        (str "#<Contact " (.current nick-subject) ">")))))

(defn tuple->contact [^Tuple tuple parties]
  (reify-contact (.payload tuple)
                 (produce-party! parties (.get tuple "party"))))

(defn restore-contact-list [^TupleSpace tuple-space own-puk parties]
  (->>
   (.. tuple-space
       filter
       (author own-puk)
       (type "contact")
       localTuples
       toBlocking
       toIterable)
   (mapcat (fn [^Tuple tuple] [(.get tuple "party") (tuple->contact tuple parties)]))
   (apply hash-map)))

(defn current-nickname [^Contact contact]
  (.. contact nickname current))
