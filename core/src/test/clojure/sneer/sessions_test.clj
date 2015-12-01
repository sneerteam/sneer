(ns sneer.sessions-test
  (:require [rx.lang.clojure.core :as rx]
            [sneer.convos]
            [sneer.sessions]
            [sneer.rx-test-util :refer :all]
            [sneer.test-util :refer :all]
            [sneer.neide-and-carla :refer :all]
            [midje.sweet :refer [fact facts]])
  (:import [sneer.convos Convos Sessions Sessions$Actions Convo]
           [sneer.flux Dispatcher]
           [sneer.rx Timeline]
           [rx Observable]))

(defn extract [coll & fields]
  (map (comp (apply juxt fields) ->clj-map)
       coll))

(defn sessions [sneer convo-id]
  (rx/map
    #(.sessionSummaries %)
    (.getById (sneer Convos) convo-id)))

(defn emits-sessions [fields expected]
  (emits #(= expected (apply extract % fields))))

(defn play [^Timeline timeline]
  (rx/concat (.past timeline)
             (.future timeline)))

(defn first-session [^Observable sessions]
  (->> sessions
       (rx/map first)
       (rx/filter some?)))

(facts "About sessions"
  (let [{:keys [neide carla ^Convo n->c ^Convo c->n]} (neide-and-carla)
        neide-sessions #(sessions neide (.id n->c))
        carla-sessions #(sessions carla (.id c->n))]
    (with-open [neide neide
                carla carla]

      (fact "In the beginning there were no sessions"
        (neide-sessions) => (emits empty?))

      (fact "And then Neide said, start a session"
        (let [start-session   (Sessions$Actions/startSession (.id n->c) "candy-crush")
              n->c-session-id (<next (.request (neide Dispatcher) start-session))]
          (neide-sessions) => (emits-sessions [:id :type]
                                              [[n->c-session-id "candy-crush"]])))

      (fact "Carla sees a session and sees that it is good"
        (carla-sessions) => (emits-sessions [:type]
                                            [["candy-crush"]]))

      (fact "Carla sees unread session summary"
        (let [c-convos (carla Convos)]
          (. c-convos summaries) => (emits #(-> % (extract :nickname :textPreview :unread)
                                                  (= [["Neide" "candy-crush" "*"]])))))

      (fact "Messages are exchanged"
        (let [n->c-session (<next (first-session (neide-sessions)))
              c->n-session (<next (first-session (carla-sessions)))]

          (let [c-timeline (.messages (carla Sessions) (.id c->n-session))
                n-timeline (.messages (neide Sessions) (.id n->c-session))]
            (.past c-timeline) => completes
            (.past n-timeline) => completes

            (.dispatch (neide Dispatcher)
                       (Sessions$Actions/sendMessage (.id n->c-session) "candy"))

            (rx/map ->clj-map (.future n-timeline)) => (emits {:payload "candy" :isOwn true })
            (rx/map ->clj-map (.future c-timeline)) => (emits {:payload "candy" :isOwn false}))

          (let [c-timeline (.messages (carla Sessions) (.id c->n-session))
                n-timeline (.messages (neide Sessions) (.id n->c-session))]
            (rx/map ->clj-map (.past   n-timeline)) => (emits {:payload "candy" :isOwn true })
            (rx/map ->clj-map (.past   c-timeline)) => (emits {:payload "candy" :isOwn false})
            (.past n-timeline) => completes
            (.past c-timeline) => completes

            (.dispatch (carla Dispatcher)
                       (Sessions$Actions/sendMessage (.id c->n-session) "crush"))

            (rx/map ->clj-map (.future n-timeline)) => (emits {:payload "crush" :isOwn false})
            (rx/map ->clj-map (.future c-timeline)) => (emits {:payload "crush" :isOwn true })            ))))))
