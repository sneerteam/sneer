(ns sneer.core2
  (require [sneer.util :refer [handle prepend]]
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

(defmethod handle :contact-new [state event]
  (let [nick (event :nick)
        id (event :id)
        summary {:contact-id    id
                 :nick          nick
                 :last-event-id id}]
    (-> state
      (update-in [:summaries :id->summary] assoc id summary))))

(defmethod handle :contact-delete [state event]
  (update-in state [:summaries :id->summary] dissoc (:contact-id event)))

(defmethod handle :contact-rename [state event]
  (let [id (:contact-id event)
        new-nick (:new-nick event)]
    (assoc-in state [:summaries :id->summary id :nick] new-nick)))

(defmethod handle :msg-send [state event]
  (let [contact-id (event :contact-id)]
    (-> state
      (assoc-in [:summaries :id->summary contact-id :preview]       (event :text))
      (assoc-in [:summaries :id->summary contact-id :last-event-id] (event :id)))))

(defn- convo-list [model]
  (->> model :summaries :id->summary vals (sort-by :last-event-id) reverse vec))

(defn- chat [streems contact-id]
  (catch-up! streems conj [] contact-id))

(defn- view [sneer model [activity contact-id]]
  (cond-> {:convo-list (convo-list model)
           :profile (:profile model)}
    (= activity :convo)
    (assoc :convo {:contact-id contact-id
                   :nick (get-in model [:summaries :id->summary contact-id :nick])
                   :chat (chat (sneer :streems) contact-id)})))

(defn- update-ui [sneer]
  (let [model (catch-up! (sneer :streems) handle)]
    ((sneer :ui-fn) (view sneer model @(sneer :view-path)))))

(defn- streem-id [event]
  (case (event :type)
    :msg-send (event :contact-id)
    nil))

(defn handle! [sneer event]
  (if (= (event :type) :view)
    (reset! (sneer :view-path) (event :path))
    (append! (sneer :streems) event (streem-id event)))
  (update-ui sneer))

(defn sneer [ui-fn streems]
  (doto
    {:ui-fn     ui-fn
     :streems   streems
     :view-path (atom nil)}
    (update-ui)))
