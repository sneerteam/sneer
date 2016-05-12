(ns reagent-spike.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [reagent-spike.client :as client]
              [cognitect.transit :as t]))

(defn- dispatch! [event]
  (client/chsk-send! [:sneer/handle event]))


;; ------------------------- Views
(def action (atom nil))

(defn- choose-view [data]
  (cond
    (:nick-validation data) :contact-new
    (:convo data)           :convo
    :else                   :convo-list))

(defmulti sneer-view choose-view)

(defn- send-invite []
  )

(defmethod sneer-view :contact-new [data]
  (let [nick (-> data :nick-validation :nick)]
    [:div {:class "input-group"}
     [:span {:class "input-group-addon" :id "basic-addon1"} "Name"]
     [:input {:type             "text"
              :class            "form-control"
              :aria-describedby "basic-addon1"
              :placeholder      "Mom, John Smith, etc"
              :value            nick
              :on-change        #(dispatch! {:type :view, :nick-validation (-> % .-target .-value)})}]
     [:button {:on-click #(dispatch! {:type :contact-new, :nick nick})} "SEND INVITE >"]]))

(defmethod sneer-view :convo [data]
  [:ul
   (for [msg (-> data :convo :message-list)]
     ^{:key (msg :id)} [:li (msg :text) " - " (msg :date)])])

#_{"id"       1000
   "nickname" "Neide 0"
   "preview"  "Hi There! 0"
   "date"     "Today 0"
   "unread"   ""}
(defmethod sneer-view :convo-list [data]
  [:div
   [:button {:on-click #(dispatch! {:todo "Switch to contact-new view without dispatching."})} "New Contact"]
   [:div
    (for [convo (data :convo-list)]
      ^{:key (convo :id)}
      [:div [:a {:on-click #(dispatch! {:type :view :convo (convo :id)})
                 :href     (str "#" (convo :id))}
             "Nick " (convo :nickname) " - " (convo :unread)]])]])

(defn- next-sim []
  (dispatch! {:type :sim-next}))

(defn home-page []
  (let [data @client/view]
    (when-let [toast (data :toast)]
      (.info js/toastr toast))
    [:div.row
     [:div.col-md-4.col-sm-6.col-xs-12   [:h2 (name (choose-view data)) " - " (str @action)]]
     [:div.col-md-4.col-sm-6.col-xs-12   [:button {:on-click next-sim} "Next Sim"]]
     [:div.col-md-12.col-sm-12.col-xs-12 [sneer-view data]]])) ;;Notice sneer-view function is not invoked with (), shortcut from reagent, see: https://reagent-project.github.io/

(defn about-page []
  [:div [:h2 "About reagent-spike"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div
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
