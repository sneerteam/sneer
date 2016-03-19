(ns sneer.core2
  (require [sneer.contact :as contact :refer [contact]]
           [sneer.view :as view]
           [sneer.util :refer [handle]]
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
  (let [contact (contact event)]
    (-> state
      (update-in [:model :contacts] contact/add contact)
      (update-in [:view] view/add-contact contact))))

(defn- view [sneer]
  (@(:state sneer) :view))

(defn- update-ui! [sneer]
  ((sneer :ui-fn) (view sneer)))

(defn handle! [sneer event]
  (let [old-view (view sneer)]
    (swap! (sneer :state) handle event)
    (when-not (= (view sneer) old-view)
      (update-ui! sneer))))

; State schema:
#_{:ui-fn fn
   :view {:convo-list [1 2 3]}
   :model {:next-id 0
           :contacts [1 2 3]}}

(defn- restore! [streems]
  (let [initial {:view view/initial}]
    (reduce handle initial (streem streems 0))))

(defn sneer [ui-fn streems]
  (doto
    {:ui-fn   ui-fn
     :streems streems
     :state   (atom (restore! streems))}
    (update-ui!)))