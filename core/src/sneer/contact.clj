(ns sneer.contact
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [atom->observable]]
   [sneer.party :refer [party-puk name-subject produce-party!]])
  (:import
   [sneer Contact]
   [sneer.rx ObservedSubject]
   [sneer.commons.exceptions FriendlyException]
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

(defn tuple->contact [^Tuple tuple puk->party]
  (reify-contact (.payload tuple)
                 (produce-party! puk->party (.get tuple "party"))))

(defn restore-contact-list [^TupleSpace tuple-space own-puk puk->party]
  (->>
   (.. tuple-space
       filter
       (author own-puk)
       (type "contact")
       localTuples
       toBlocking
       toIterable)
   (mapcat (fn [^Tuple tuple] [(.get tuple "party") (tuple->contact tuple puk->party)]))
   (apply hash-map)))

(defn current-nickname [^Contact contact]
  (.. contact nickname current))

(defn create-contact-state [tuple-space own-puk puk->party]
  (let [puk->contact (atom (restore-contact-list tuple-space own-puk puk->party))
        ->contact-list (fn [contact-map] (->> contact-map vals (sort-by current-nickname) vec))]
    {:puk->contact puk->contact   
     :observable-contacts (rx/map ->contact-list (atom->observable puk->contact))}))

(defn add-contact [contact-state tuple-space nickname party own-puk]
  (swap! (contact-state :puk->contact)
    (fn [cur]
      (when (->> cur vals (some (partial duplicate-contact? nickname party)))
        (throw (FriendlyException. "Duplicate contact!")))
      (assoc cur
        (party-puk party)
        (reify-contact nickname party))))
  (.. tuple-space
      publisher
      (audience own-puk)
      (type "contact")
      (field "party" (party-puk party))
      (pub nickname)))

(defn get-contacts [contact-state]
  (:observable-contacts contact-state))

(defn find-contact [contact-state party]
  (get @(:puk->contact contact-state) (party-puk party)))

(defn problem-with-new-nickname [contact-state new-nick])
