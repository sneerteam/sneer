(ns sneer.model
  (require
    [sneer.invite :as invite]
    [sneer.util.core :refer [handle prepend assoc-some conj-vec remove-vec]]
    [sneer.streem :refer :all]))

#_(defn- message-sim [n]
  {:id     (+ 10000 n)
   :text   (str "Hi There! " n)
   :date   (str "Today " n)
   :is-own (zero? (mod n 3))})

#_(defn- convo-sim [n]
  {:id       (+ 1000 n)
   :nickname (str "Neide " n)
   :preview  (str "Hi There! " n)
   :date     (str "Today " n)
   :unread   (get unreads (mod n 3))})

#_(defn- convos-view-sim [count]
  {:view :convos
   :convo-list (convo-sims count)})

#_(defn- convo-view-sim [count]
  {:view :convo
   :id 1042
   :tab :chat
   :message-list (message-sims count)})

(defmethod handle :own-name-set [state event]
  (let [own-name (event :own-name)]
    (assoc-in state [:profile :own-name] own-name)))

(defn summary-append [state summary]
  (let [id (summary :contact-id)
        summary (assoc summary :last-event-id id)]
    (update-in state [:convos :id->summary] assoc id summary)))

(defn puk [state]
  (get-in state [:key-pair :puk]))

(defn- own-name [state]
  (get-in state [:profile :own-name]))

(defn- invite [state random-long]
  (invite/encode {:puk   (puk state)
                  :name  (own-name state)
                  :nonce random-long}))

(defn- contact-id-given [state atribute value]
  (->> state
    :convos
    :id->summary
    vals
    (filter #(-> % atribute (= value)))
    first
    :contact-id))

(defn problem-with-nickname
  ([state new-nick]
    (problem-with-nickname state nil new-nick))
  ([state old-nick new-nick]
    (cond
      (or (nil? new-nick) (.isEmpty ^String new-nick))
      "cannot be empty"

      (= old-nick new-nick)
      nil

      (some? (contact-id-given state :nick new-nick))
      "already used")))

(defmethod handle :contact-new [state event]
  (let [nick (event :nick)]
    (if (problem-with-nickname state nick)
      state
      (summary-append state {:contact-id (event :id)
                             :nick       nick
                             :invite     (invite state (event :random-bytes))}))))

(defmethod handle :contact-delete [state event]
  (update-in state [:convos :id->summary] dissoc (:contact-id event)))

(defmethod handle :contact-rename [state event]
  (let [id (:contact-id event)
        new-nick (:new-nick event)]
    (assoc-in state [:convos :id->summary id :nick] new-nick)))

(defmethod handle :contact-invite-thanks [state event]
  (let [invite (:invite event)
        from   (:from event)]
    (if-some [contact-id (contact-id-given state :invite invite)]
      (-> state
        (update-in [:convos :id->summary contact-id] dissoc :invite)
        (update-in [:convos :id->summary contact-id] assoc :puk from))
      state)))

(defn- thank-for-invite [state puk invite]
  (let [event {:type   :contact-invite-thanks
               :invite invite}]
    (update-in state [:network :events-out] conj-vec {:to puk, :event event})))

(defmethod handle :contact-invite-accept [state event]
  (let [invite (event :invite)
        {:keys [name puk]} (invite/decode invite)]
    (-> state
      (summary-append {:contact-id (event :id)
                       :nick       name
                       :puk        puk})
      (thank-for-invite puk invite))))

(defmethod handle :msg-send [state event]
  (let [contact-id (event :contact-id)]
    (-> state
      (assoc-in [:convos :id->summary contact-id :preview]       (event :text))
      (assoc-in [:convos :id->summary contact-id :last-event-id] (event :id)))))

(defmethod handle :keys-init [state event]
  (assoc state :key-pair (select-keys event [:prik :puk])))


;================== NETWORK

(defmethod handle :event-sent [state event-sent]
  (let [event-out (:event event-sent)]
    (update-in state [:network :events-out] remove-vec event-out)))

#_{:type :event-in
   :event {:from puk
           :type :some-type
           :some-attribute :some-value}}
(defmethod handle :event-in [state event-in]
  (let [event (:event event-in)]
    (if (:from event)
      (handle state event)
      (do
        (println "Ignoring event without sender:" event)
        state))))