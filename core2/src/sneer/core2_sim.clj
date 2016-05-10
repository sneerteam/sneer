(ns sneer.core2-sim)

(defn- message-sim [n]
  {:id     (+ 10000 n)
   :text   (str "Hi There! " n)
   :date   (str "Today " n)
   :is-own (zero? (mod n 3))})

(def ^:private unreads ["" "*" "?"])

(defn- convo-sim [n]
  {:id       (+ 1000 n)
   :nickname (str "Neide " n)
   :preview  (str "Hi There! " n)
   :date     (str "Today " n)
   :unread   (get unreads (mod n 3))})

(defn- convo-sims [count]
  (map convo-sim (range count)))

(defn- message-sims [count]
  (map message-sim (range count)))

(defn- convos-view-sim [count]
  {:convo-list (convo-sims count)})

(defn- convo-view-sim [count]
  {:convo-list (convo-sims 100)
   :convo {:id 1042
           :tab :chat
           :message-list (message-sims count)}})

(def ^:private view-sims
  (cycle
    [(convos-view-sim 0)
     (convos-view-sim 1)
     (convos-view-sim 100)
     (convo-view-sim  0)
     (convo-view-sim  1)
     (convo-view-sim  1000)]))

(defn- handle-sim [state]
  (-> state
    (assoc :view (first (state :view-sims)))
    (update :view-sims rest)))

(defn- handle-event [state event]
  (assoc-in state [:view :toast] (str event)))

(defn- handle [state event]
  (if (= (event :type) :sim-next)
    (handle-sim state)
    (handle-event state event)))

(defn handle! [sneer event]
  (swap! sneer handle event)
  ((@sneer :ui-fn) (@sneer :view)))

(defn sneer-simulator [ui-fn]
  (atom {:ui-fn     ui-fn
         :view-sims view-sims}))