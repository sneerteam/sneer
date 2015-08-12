(ns sneer.notifications-test
  (:require [midje.sweet :refer :all]
            [sneer.test-util :refer :all]
            [sneer.integration-test-util :refer :all]
            [sneer.notifications :refer :all])
  (:import [sneer.flux Dispatcher]
           [sneer.convos Convos Notifications Notifications$Notification]))

(defn- neide-and-carla []
  (let [neide (sneer!)
        carla (sneer!)
        _     (connect! neide carla)
        n-convos ^Convos (neide Convos)
        n->c-id  (<next (. n-convos startConvo "Carla"))
        n->c     (<next (.getById n-convos n->c-id))
        c-convos ^Convos (carla Convos)
        c->n-id  (.acceptInvite c-convos
                                "Neide"
                                (.ownPuk n-convos)
                                (.inviteCodePending n->c))
        c->n-id  (<next (.findConvo c-convos (.ownPuk n-convos)))
        c->n     (<next (.getById c-convos c->n-id))]
    {:neide neide
     :n->c n->c
     :carla carla
     :c->n c->n}))

(facts "About notifications"
  (let [{:keys [neide carla n->c c->n]} (neide-and-carla)]
    (with-open [neide neide
                carla carla]
      (let [notifications (neide Notifications)
            send-message #(.dispatch (carla Dispatcher)
                                     (.sendMessage c->n %))]

        (fact "Starts empty"
          (<next (.get notifications)) => nil)

        (fact "New message causes new notification"
          (send-message "hi")
          (.get notifications) => (emits #(= (.text %) "hi")))))))
