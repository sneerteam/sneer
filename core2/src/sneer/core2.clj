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

(defmethod handle :contact-new [state event]
  (let [nick (event :nick)
        id (event :id)
        contact {:contact-id id
                 :nick       nick}]
    (-> state
      (update-in [:contacts :id->contact] assoc id contact))))

(defmethod handle :contact-delete [state event]
  (update-in state [:contacts :id->contact] dissoc (:contact-id event)))

(defmethod handle :contact-rename [state event]
  (let [id (:contact-id event)
        new-nick (:new-nick event)]
    (assoc-in state [:contacts :id->contact id :nick] new-nick)))

(defmethod handle :message-send [state event]
  ;(println "MESSAGE SEND DOES NOTHING FOR NOW. MUST UPDATE CONVO LIST")
  state)

(defn convo-list [model]
  (->> model :contacts :id->contact vals (sort-by :contact-id) reverse vec))

(defn chat [streems contact-id]
  (catch-up! streems conj [] contact-id))

(defn- view [sneer model [activity contact-id]]
  (cond-> {:convo-list (convo-list model)}
    (= activity :convo)
    (assoc :convo {:contact-id contact-id
                   :nick (get-in model [:contacts :id->contact contact-id :nick])
                   :chat (chat (sneer :streems) contact-id)})))

(defn- update-ui [sneer]
  (let [model (catch-up! (sneer :streems) handle)]
    ((sneer :ui-fn) (view sneer model @(sneer :view-path)))))

(defn- streem-id [event]
  (case (event :type)
    :message-send (event :contact-id)
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
