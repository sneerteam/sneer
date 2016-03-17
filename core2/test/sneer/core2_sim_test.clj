(ns sneer.core2-sim-test
  (:use midje.sweet)
  (:use [sneer.core2-sim]))

(facts "UI Sims"
  (let [fake-ui (atom nil)
        subject (sneer-simulator #(reset! fake-ui %))]

    (fact "Events are simulated as toasts"
      (dispatch! subject {:type :some-event
                          :some-arg 42})
      (@fake-ui :toast) => "{:type :some-event, :some-arg 42}")

    (fact "View sims are generated"
      (dispatch! subject {:type :sim-next})
      @fake-ui => {:view :convos
                   :convo-list []}

      (dispatch! subject {:type :sim-next})
      @fake-ui => {:view :convos
                   :convo-list [{:id       1000
                                 :nickname "Neide 0"
                                 :preview  "Hi There! 0"
                                 :date     "Today 0"
                                 :unread   ""}]}

      (dispatch! subject {:type :sim-next})
      (count (@fake-ui :convo-list)) => 100)


    (fact "Message sims are generated"
      (dispatch! subject {:type :sim-next})
      @fake-ui => {:view :convo
                   :id 1042
                   :tab :chat
                   :message-list []}

      (dispatch! subject {:type :sim-next})
      @fake-ui => {:view :convo
                   :id 1042
                   :tab :chat
                   :message-list [{:id     10000
                                   :is-own true
                                   :text   "Hi There! 0"
                                   :date   "Today 0"}]}

      (dispatch! subject {:type :sim-next})
      (count (@fake-ui :message-list)) => 10000)))