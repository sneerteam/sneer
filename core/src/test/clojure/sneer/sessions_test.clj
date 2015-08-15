(ns sneer.sessions-test
  (:require [rx.lang.clojure.core :as rx]
            [sneer.convos]
            [sneer.test-util :refer :all]
            [sneer.neide-and-carla :refer :all]
            [midje.sweet :refer :all])
  (:import [sneer.convos Convos SessionSummary]
           [sneer.flux Dispatcher]))

(defn sessions [sneer convo-id]
  (rx/map
   #(.sessionSummaries %)
   (.getById (sneer Convos) convo-id)))

(defn session-id [^SessionSummary ss]
  (.id ss))

(defn session-title [^SessionSummary ss]
  (.title ss))

(facts "About sessions"
  (let [{:keys [neide carla n->c c->n]} (neide-and-carla)
        neide-sessions #(sessions neide (.id n->c))]
    (with-open [neide neide
                carla carla]

      (fact "In the beginning there were no sessions"
        (neide-sessions) => (emits empty?))

      (fact "And then Neide said, start a session"
        (let [start-session   (.startSession n->c "candy-crush")
              n->c-session-id (<next (.request (neide Dispatcher) start-session))]
          (neide-sessions) => (emits #(->> %
                                           (map (juxt session-id session-title))
                                           (= [[n->c-session-id "candy-crush"]]))))))))
