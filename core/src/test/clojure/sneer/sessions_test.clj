(ns sneer.sessions-test
  (:require [rx.lang.clojure.core :as rx]
            [sneer.convos]
            [sneer.test-util :refer :all]
            [sneer.neide-and-carla :refer :all]
            [midje.sweet :refer :all])
  (:import [sneer.convos Convos Sessions$Actions]
           [sneer.flux Dispatcher]))

(defn extract [coll & fields]
  (map (comp (apply juxt fields) ->clj)
       coll))

(defn sessions [sneer convo-id]
  (rx/map
    #(.sessionSummaries %)
    (.getById (sneer Convos) convo-id)))

(defn emits-sessions [fields expected]
  (emits #(= expected (apply extract % fields))))

(facts "About sessions"
  (let [{:keys [neide carla n->c c->n]} (neide-and-carla)
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
                                                  (= [["Neide" "candy-crush" "*"]]))))))))