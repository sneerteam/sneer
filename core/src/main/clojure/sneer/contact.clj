(ns sneer.contact
  (:require
    [rx.lang.clojure.core :as rx]
    [sneer.rx :refer [atom->observable]]
    [sneer.party :refer [party-puk produce-party!]]
    [sneer.party-impl :refer [name-subject]])
  (:import
    [sneer Contact PublicKey Party]
    [sneer.rx ObservedSubject]
    [sneer.commons.exceptions FriendlyException]
    [sneer.tuples Tuple TupleSpace]))

(defn publish-contact-without-party [tuple-space own-puk new-nick invite-code]
  (.. ^TupleSpace tuple-space
      publisher
      (audience own-puk)
      (type "contact")
      (field "invite-code" invite-code)
      (pub new-nick)))

(defn publish-contact [tuple-space own-puk new-nick party]
  (.. ^TupleSpace tuple-space
      publisher
      (audience own-puk)
      (type "contact")
      (field "party" (party-puk party))
      (pub new-nick)))

(defn problem-with-new-nickname-in [puk->contact puk new-nick]
  (let [puk->contact (dissoc puk->contact puk)]
    (cond
      (.isEmpty ^String new-nick) "cannot be empty"
      (->> puk->contact vals (some #(= new-nick (.. ^Contact % nickname current)))) "already used")))

(defn problem-with-new-nickname [contacts-state puk new-nick]
  (problem-with-new-nickname-in @(:puk->contact contacts-state) puk new-nick))

(defn check-new-nickname [puk->contact puk new-nick]
  (when-let [problem (problem-with-new-nickname-in puk->contact puk new-nick)]
    (throw (FriendlyException. (str "Nickname " problem)))))

(defn reify-contact [tuple-space puk->contact own-puk nickname party]
  (let [nick-subject (ObservedSubject/create nickname)]
    (.subscribe ^rx.Observable (.observable nick-subject) ^ObservedSubject (name-subject party))
    (reify Contact
      (party [_] party)

      (nickname [_]
        (.observed nick-subject))

      (setNickname [_ new-nick]
        (check-new-nickname @puk->contact (party-puk party) new-nick)
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

(defn- ->contact-list [contact-map]
  (->> contact-map vals (sort-by current-nickname) vec))

(defn create-contacts-state [tuple-space own-puk puk->party]
  (let [puk->contact (atom {})
        nick->contact (atom (restore-contact-list tuple-space puk->contact own-puk puk->party))
        nicks-without-puk (map first (remove #(-> % second .party) @nick->contact))]
    (reset! puk->contact (apply dissoc @nick->contact nicks-without-puk))
    {:own-puk             own-puk
     :tuple-space         tuple-space
     :puk->contact        puk->contact
     :nick->contact       nick->contact
     :observable-contacts (rx/map ->contact-list (atom->observable nick->contact))}))

(defn find-contact-in [puk->contact party]
  (get puk->contact (party-puk party)))

(defn find-contact [contacts-state party]
  (find-contact-in @(:puk->contact contacts-state) party))

(defn check-new-contact [puk->contact nickname party]
  (when (find-contact-in puk->contact party)
    (throw (FriendlyException. "Duplicate contact")))
  (check-new-nickname puk->contact (.. ^Party party publicKey current) nickname))

(defn add-contact-without-party [contacts-state nickname invite-code]
  (println nickname "was added to your contacts. Invite code =>" invite-code)
  (publish-contact-without-party (:tuple-space contacts-state) (:own-puk contacts-state) nickname invite-code))

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
