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

(defn publish-contact [tuple-space own-puk new-nick party]  
  (.. tuple-space
      publisher
      (audience own-puk)
      (type "contact")
      (field "party" (party-puk party))
      (pub new-nick)))

(defn problem-with-new-nickname-in [puk->contact new-nick]
  (cond
    (.isEmpty new-nick) "Cannot be empty"
    (->> puk->contact vals (some #(= new-nick (.. % nickname current)))) "Already used"))

(defn problem-with-new-nickname [contacts-state new-nick]
  (problem-with-new-nickname-in @(:puk->contact contacts-state) new-nick))

(defn check-new-nickname [puk->contact new-nick]
  (when-let [problem (problem-with-new-nickname-in puk->contact new-nick)]
    (throw (FriendlyException. (str "Nickname " problem)))))

(defn reify-contact [tuple-space puk->contact own-puk nickname party]
  (let [nick-subject (ObservedSubject/create nickname)]
    (.subscribe (.observable nick-subject) (name-subject party))
    (reify Contact
      (party [this] party)
      
      (nickname [this]
        (.observed nick-subject))
      
      (setNickname [this new-nick]
        (check-new-nickname @puk->contact new-nick)
        (publish-contact tuple-space own-puk new-nick party)
        (rx/on-next nick-subject new-nick))
      
      (toString [this]
        (str "#<Contact " (.current nick-subject) ">")))))

(defn tuple->contact [tuple-space puk->contact ^Tuple tuple puk->party]
  (reify-contact tuple-space
                 puk->contact
                 (.author tuple)
                 (.payload tuple)
                 (produce-party! puk->party (.get tuple "party"))))

(defn restore-contact-list [^TupleSpace tuple-space puk->contact own-puk puk->party]
  (->>
   (.. tuple-space
       filter
       (author own-puk)
       (type "contact")
       localTuples
       toBlocking
       toIterable)
   (mapcat (fn [^Tuple tuple] [(.get tuple "party") (tuple->contact tuple-space puk->contact tuple puk->party)]))
   (apply hash-map)))

(defn current-nickname [^Contact contact]
  (.. contact nickname current))

(defn create-contacts-state [tuple-space own-puk puk->party]
  (let [puk->contact (atom {})
        ->contact-list (fn [contact-map] (->> contact-map vals (sort-by current-nickname) vec))]
    (swap! puk->contact (fn [_] (restore-contact-list tuple-space puk->contact own-puk puk->party)))
    {:own-puk own-puk
     :tuple-space tuple-space
     :puk->contact puk->contact
     :observable-contacts (rx/map ->contact-list (atom->observable puk->contact))}))

(defn find-contact-in [puk->contact party]
  (get puk->contact (party-puk party)))

(defn find-contact [contacts-state party]
  (find-contact-in @(:puk->contact contacts-state) party))

(defn check-new-contact [puk->contact nickname party]
  (when (find-contact-in puk->contact party)
    (throw (FriendlyException. "Duplicate contact")))
  (check-new-nickname puk->contact nickname))

(defn add-contact [contacts-state nickname party]
  (swap! (contacts-state :puk->contact)
    (fn [cur]     
      (check-new-contact cur nickname party)
      (assoc cur
        (party-puk party)
        (reify-contact (:tuple-space contacts-state) (:puk->contact contacts-state) (:own-puk contacts-state) nickname party))))
  (publish-contact (:tuple-space contacts-state) (:own-puk contacts-state) nickname party))

(defn get-contacts [contacts-state]
  (:observable-contacts contacts-state))
