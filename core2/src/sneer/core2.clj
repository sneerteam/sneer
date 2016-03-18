(ns sneer.core2
  (require [sneer.util :refer [handle]]
           [sneer.contact :refer :all]))

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

(defn- update-ui [sneer]
  ((@sneer :ui-fn) (@sneer :view)))

(defn handle! [sneer event]
  (let [old-view (@sneer :view)]

    (swap! sneer handle event)

    (when-not (= (@sneer :view) old-view)
      (update-ui sneer))))

(defn sneer [ui-fn]
  (doto
    (atom {:ui-fn ui-fn
           :view  {:convo-list []}})
    (update-ui)))