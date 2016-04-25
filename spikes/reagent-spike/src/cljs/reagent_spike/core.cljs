(ns reagent-spike.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [reagent-spike.client :as client]
              [cognitect.transit :as t]))

;; -------------------------
;; Views
(def action (atom nil))




(defn- choose-view [data]
  (cond (:convo data) :convo
        :else         :convo-list))

(defmulti sneer-view choose-view)

(defmethod sneer-view :convo [data]
  [:ul
   (for [msg (-> data :convo :message-list)]
     ^{:key (msg :id)} [:li (msg :text) " - " (msg :date)])])

(defn- dispatch! [event]
  (.log js/console event)
  (.log js/console (t/write (t/writer :json) {:foo {:bar "~:str"}}))
  (reset! action event))

#_{"id"       1000
   "nickname" "Neide 0"
   "preview"  "Hi There! 0"
   "date"     "Today 0"
   "unread"   ""}
(defmethod sneer-view :convo-list [data]
  [:ul
   (for [convo (data :convo-list)]
     ^{:key (convo :id)} [:li {:on-click #(dispatch! (convo :id))} "Nick " (convo :nickname) " - " (convo :unread)])])

(defn- next-sim []
  (client/chsk-send! [:sneer/handle {:type :sim-next}]))

(defn home-page []
  (let [data @client/view]

    [:div.row
     [:div.col-md-4.col-sm-6.col-xs-12   [:h2 (name (choose-view data)) " - " (str @action)]]
     [:div.col-md-4.col-sm-6.col-xs-12   [:button {:on-click next-sim} "Next Sim"]]
     [:div.col-md-12.col-sm-12.col-xs-12 [sneer-view data]]])) ;;Notice sneer-view function is not invoked with (), shortcut from reagent, see: https://reagent-project.github.io/

(defn about-page []
  [:div [:h2 "About reagent-spike"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div
   ;;Bootstrap
   [:link {:rel         "stylesheet"
           :href        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"
           :integrity   "sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7"
           :crossOrigin "anonymous"}]
   [:link {:rel         "stylesheet"
           :href        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css"
           :integrity   "sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r"
           :crossOrigin "anonymous"}]
   [:script
          {:src        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"
           :integrity   "sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS"
           :crossOrigin "anonymous"}]
   ;;Content
   [:div [(session/get :current-page)]]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/main.html" []
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
