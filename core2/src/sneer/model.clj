(ns sneer.model
  (require
    [sneer.invite :as invite]
    [sneer.util.core :refer [handle prepend assoc-some]]
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

(defn puk2 [state]
  (get-in state [:key-pair :puk]))

(defn- own-name [state]
  (get-in state [:profile :own-name]))

(defn- invite [state random-long]
  (invite/encode {:puk   (puk2 state)
                  :name  (own-name state)
                  :nonce random-long}))

(defmethod handle :contact-new [state event]
  (summary-append state
    {:contact-id (event :id)
     :nick       (event :nick)
     :invite     (invite state (event :random-bytes))}))

(defmethod handle :contact-delete [state event]
  (update-in state [:convos :id->summary] dissoc (:contact-id event)))

(defmethod handle :contact-rename [state event]
  (let [id (:contact-id event)
        new-nick (:new-nick event)]
    (assoc-in state [:convos :id->summary id :nick] new-nick)))

(defn- invite->contact-id [state invite]
  (->> state
    :convos
    :id->summary
    vals
    (some #(-> % :invite (= invite)))
    :contact-id))

(defmethod handle :contact-invite-accept [state event]
  (let [invite (invite/decode (event :invite))]
    (-> state
      (summary-append {:contact-id (event :id)
                       :nick       (invite :name)
                       :puk        (invite :puk)})
      ))

  ; THE FOLLOWING MUST HAPPEN IN THE SENDER:
  #_(let [invite (:invite event)
        contact-id (invite->contact-id state invite)]
    (update-in state [:convos :id->summary contact-id] dissoc :invite)))

(defmethod handle :msg-send [state event]
  (let [contact-id (event :contact-id)]
    (-> state
      (assoc-in [:convos :id->summary contact-id :preview]       (event :text))
      (assoc-in [:convos :id->summary contact-id :last-event-id] (event :id)))))

(defmethod handle :keys-init [state event]
  (assoc state :key-pair (select-keys event [:prik :puk])))
