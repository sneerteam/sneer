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
  (let [contact {:contact-id (event :id)
                 :nick       (event :nick)}]
    (-> state
      (update-in [:convo-list] prepend contact)
      (assoc :convo []))))

(defn- remove-contact [contacts contact-id]
  (vec (remove #(= (:contact-id %) contact-id) contacts)))

(defmethod handle :contact-delete [state event]
  (let [contact-id (:contact-id event)]
    (update-in state [:convo-list] #(remove-contact % contact-id))))

(defn- rename-contact [contacts contact-id new-nick]
  (vec (map #(if (= (% :contact-id) contact-id)
              (assoc % :nick new-nick)
              %)
            contacts)))

(defmethod handle :contact-rename [state event]
  (let [contact-id (:contact-id event)
        new-nick (:new-nick event)]
    (update-in state [:convo-list] #(rename-contact % contact-id new-nick))))

(defn view [streems]
  (restore! streems handle {:convo-list (list)}))

(defn- update-ui [sneer]
  ((sneer :ui-fn) (view (sneer :streems))))

(defn handle! [sneer event]
  (append (sneer :streems) event)
  (update-ui sneer))

(defn sneer [ui-fn streems]
  (doto
    {:ui-fn   ui-fn
     :streems streems}
    (update-ui)))
