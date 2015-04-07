(ns sneer.contact
  (:require
    [rx.lang.clojure.core :as rx]
    [sneer.rx :refer [atom->observable combine-latest]]
    [sneer.party :refer [party-puk produce-party!]]
    [sneer.party-impl :refer [name-subject]])
  (:import
    [java.util UUID]
    [sneer Contact PublicKey Party]
    [sneer.rx ObservedSubject]
    [sneer.commons.exceptions FriendlyException]
    [sneer.tuples Tuple TupleSpace]))

(defn publish-contact [tuple-space own-puk new-nick party invite-code]
  (.. ^TupleSpace tuple-space
      publisher
      (audience own-puk)
      (type "contact")
      (field "party" (when party (party-puk party)))
      (field "invite-code" invite-code)
      (pub new-nick)))

(defn problem-with-new-nickname-in [puk->contact new-nick party]
  (let [puk (some-> ^Party party .publicKey .current)
        puk->contact (dissoc puk->contact puk)]
    (cond
      (.isEmpty ^String new-nick)
      "cannot be empty"

      (->> puk->contact
           vals
           (some #(and (some? (.party %))
                       (= new-nick (.. ^Contact % nickname current)))))
      "already used")))

(defn problem-with-new-nickname [contacts-state new-nick party]
  (problem-with-new-nickname-in @(:puk->contact contacts-state) new-nick party))

(defn check-new-nickname [puk->contact new-nick party]
  (when-let [problem (problem-with-new-nickname-in puk->contact new-nick party)]
    (throw (FriendlyException. (str "Nickname " problem)))))

(defn reify-contact
  [tuple-space puk->contact own-puk nickname party invite-code]
  (let [nick-subject (ObservedSubject/create nickname)]
    (when party
      (.subscribe ^rx.Observable (.observable nick-subject) ^ObservedSubject (name-subject party)))
    (reify Contact
      (party [_] party)

      (setParty [_ party] (assert false))

      (inviteCode [_] invite-code)

      (nickname [_]
        (.observed nick-subject))

      (setNickname [_ new-nick]
        (when party
          (check-new-nickname @puk->contact new-nick party))
        (publish-contact tuple-space own-puk new-nick party invite-code)
        (rx/on-next nick-subject new-nick))

      (toString [this]
        (str "#<Contact " (.current nick-subject) ">")))))

(defn tuple->contact [tuple-space puk->contact ^Tuple tuple puk->party]
  (reify-contact tuple-space
                 puk->contact
                 (.author tuple)
                 (.payload tuple)
                 (some->> (.get tuple "party") (produce-party! puk->party))
                 (.get tuple "invite-code")))

(defn restore-contacts [^TupleSpace tuple-space nick->contact puk->contact own-puk puk->party]
  (->>
    (.. tuple-space
        filter
        (author own-puk)
        (type "contact")
        localTuples
        toBlocking
        toIterable)
    #_(mapcat (fn [^Tuple tuple]
              [(.get tuple "party") (tuple->contact tuple-space puk->contact tuple puk->party)]))
    #_(apply hash-map)
    (map (fn [^Tuple tuple]
              (tuple->contact tuple-space puk->contact tuple puk->party)))))

(defn current-nickname [^Contact contact]
  (.. contact nickname current))

(defn- ->contact-list [[puk->contact nick->contact]]
  (->> (concat (vals puk->contact) (vals nick->contact))
       set
       vec
       (sort-by current-nickname)))

(defn create-contacts-state [tuple-space own-puk puk->party]
  (let [nick->contact (atom {})
        puk->contact (atom {})
        contacts (restore-contacts tuple-space nick->contact puk->contact own-puk puk->party)]

     (reset! nick->contact (apply hash-map (mapcat (fn [c] [(.nickname c) c])
                                                contacts)))
     (reset! puk->contact  (apply hash-map (->> contacts
                                                (filter #(.party %))
                                                (mapcat (fn [c] [(.. c party publicKey current) c])))))

    {:own-puk             own-puk
     :tuple-space         tuple-space
     :nick->contact       nick->contact
     :puk->contact        puk->contact
     :observable-contacts (combine-latest ->contact-list
                                          [(atom->observable puk->contact)
                                           (atom->observable nick->contact)])}))

(defn find-contact-in [puk->contact party]
  (get puk->contact (party-puk party)))

(defn find-contact [contacts-state party]
  (find-contact-in @(:puk->contact contacts-state) party))

(defn check-new-contact [puk->contact nickname party]
  (when (some->> party (find-contact-in puk->contact))
    (throw (FriendlyException. "Duplicate contact")))
  (check-new-nickname puk->contact nickname party))

(defn produce-contact [contacts-state nickname party invite-code-received]
  (let [contact (reify-contact (:tuple-space contacts-state)
                               (:puk->contact contacts-state)
                               (:own-puk contacts-state)
                               nickname
                               party
                               invite-code-received)
        invite-code (when-not party (-> (UUID/randomUUID) .toString (.replaceAll "-" "")))]
    (swap! (contacts-state (if party :puk->contact :nick->contact))
           (fn [cur]
             (check-new-contact cur nickname party)
             (assoc cur (if party (party-puk party) nickname) contact)))
    (publish-contact (:tuple-space contacts-state) (:own-puk contacts-state) nickname party invite-code)
    contact))

(defn get-contacts [contacts-state]
  (:observable-contacts contacts-state))
