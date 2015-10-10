(ns sneer.notifications-test
  (:require [midje.sweet :refer :all]
            [rx.lang.clojure.core :as rx]
            [sneer.test-util :refer :all]
            [sneer.rx-test-util :refer :all]
            [sneer.neide-and-carla :refer [neide-and-carla]]
            [sneer.notifications :refer :all])
  (:import [sneer.flux Dispatcher]
           [sneer.convos Notifications]))

(facts "About notifications"
  (binding [sneer.notifications/*debounce-timeout* 0]
    (let [{:keys [neide carla c->n]} (neide-and-carla)]
      (with-open [neide neide
                  carla carla]
        (let [notifications (neide Notifications)
              send-message #(.dispatch (carla Dispatcher)
                                       (.sendMessage c->n %))]

          (fact "Starts empty"
            (<next (.get notifications)) => nil)

          (fact "New message causes new notification"
            (send-message "hi")
            (.get notifications) => (emits #(= (.text %) "hi")))

          (fact "Question causes new notification"
            (send-message "hi?")
            (->> (.get notifications)
                 (rx/map (juxt #(.convoId %) #(.text %))))))))))
