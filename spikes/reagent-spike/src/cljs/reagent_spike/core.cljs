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
   :preview  (str "Hi There! " n)
   :date     (str "Today " n)
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

    [:div.row
     [:div.col-md-4.col-sm-6.col-xs-12  [:h2 (sim :view) " - " (str @action)]]
     [:div.col-md-4.col-sm-6.col-xs-12  [:button {:on-click #(swap! sims rest)} "Next Sim"]]
     [:div.col-md-12 [sneer-view sim]]])) ;;Notice sneer-view function is not invoked with (), shortcut from reagent, see: https://reagent-project.github.io/

(defn about-page []
  [:div [:h2 "About reagent-spike"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div
   ;;Bootstrap
   [:link {:rel         "stylesheet"
           :href        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"
           :integrity   "sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7"
           :crossorigin "anonymous"}]
   [:link {:rel         "stylesheet"
           :href        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css"
           :integrity   "sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r"
           :crossorigin "anonymous"}]
   [:script
          {:src        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"
           :integrity   "sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS"
           :crossorigin "anonymous"}]
   ;;Content
   [:div [(session/get :current-page)]]])

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
