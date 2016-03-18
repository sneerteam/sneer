(ns sneer.core2
  (require [sneer.util :refer [handle]]
           [sneer.contact :as contact :refer [contact]]
           [sneer.view :as view]))

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

(defn- update-ui [sneer]
  ((@sneer :ui-fn) (@sneer :view)))

(defn handle! [sneer event]
  (let [old-view (@sneer :view)]
    (swap! sneer handle event)
    (when-not (= (@sneer :view) old-view)
      (update-ui sneer))))

; State schema:
#_{:ui-fn fn
   :view {:convo-list [1 2 3]}
   :model {:next-id 0
           :contacts [1 2 3]}}

(defn sneer [ui-fn]
  (doto
    (atom {:ui-fn ui-fn
           :view  {:convo-list []}
           :model {:next-id 0}})
    (update-ui)))