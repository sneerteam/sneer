(ns reagent-spike.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

;; -------------------------
;; Views

(def ^:private unreads ["" "*" "?"])

(defn- convo-sim [n]
  {:id       (+ 1000 n)
   :nickname (str "Neide " n)
   "preview"  (str "Hi There! " n)
   "date"     (str "Today " n)
   :unread   (get unreads (mod n 3))})

(defn- convo-sims [count]
  (map convo-sim (range count)))

(def action (atom nil))

(def sims
  (atom (cycle [{:view "convos"
                 :convo-list (convo-sims 0)}

                {:view "convos"
                 :convo-list (convo-sims 1)}

                {:view "convos"
                 :convo-list (convo-sims 5)}

                {:view "convo"
                 :id 1042
                 "tab" "chat"
                 :message-list [{:id     10000
                                 :is-own true
                                 :text   "Hi There! 0"
                                 :date   "Today 0"}]}])))

#_{"id"       1000
   "nickname" "Neide 0"
   "preview"  "Hi There! 0"
   "date"     "Today 0"
   "unread"   ""}

(defmulti sneer-view :view)

(defn- dispatch! [event]
  (.log js/console event)
  (reset! action event))

(defmethod sneer-view "convos" [data]
  [:ul
   (for [convo (data :convo-list)]
     ^{:key (convo :id)} [:li {:on-click #(dispatch! (convo :id))} "Nick " (convo :nickname) " - " (convo :unread)])])

(defmethod sneer-view "convo" [data]
  [:ul
   (for [msg (data :message-list)]
     ^{:key (msg :id)} [:li (msg :text) " - " (msg :date)])])

(defn home-page []
  (let [sim (first @sims)]

    [:div [:h2 (sim :view) " - " (str @action)]
     [:div [:button {:on-click #(swap! sims rest)} "Next Sim"]]
     [sneer-view sim]]))

(defn about-page []
  [:div [:h2 "About reagent-spike"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
