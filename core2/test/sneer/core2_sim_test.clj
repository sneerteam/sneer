(ns sneer.core2-sim-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.test-util :refer :all]
    [sneer.core2-sim :refer :all]])

(facts "UI Sims"
  (let [fake-ui (atom nil)
        subject (sneer-simulator #(reset! fake-ui %))]

    (fact "Events are simulated as toasts"
      (handle! subject {:type :some-event
                        :some-arg 42})
      (@fake-ui :toast) => "{:type :some-event, :some-arg 42}")

    (fact "View sims are generated"
      (handle! subject {:type :sim-next})
      @fake-ui => {:convo-list []}

      (handle! subject {:type :sim-next})
      @fake-ui => {:convo-list [{:id       1000
                                 :nickname "Neide 0"
                                 :preview  "Hi There! 0"
                                 :date     "Today 0"
                                 :unread   ""}]}

      (handle! subject {:type :sim-next})
      (count (@fake-ui :convo-list)) => 100)


    (fact "Message sims are generated"
      (handle! subject {:type :sim-next})
      (count (@fake-ui :convo-list)) => 100
      (@fake-ui :convo) => {:id 1042
                            :tab :chat
                            :message-list []}

      (handle! subject {:type :sim-next})
      (@fake-ui :convo) => {:id 1042
                            :tab :chat
                            :message-list [{:id     10000
                                            :is-own true
                                            :text   "Hi There! 0"
                                            :date   "Today 0"}]}

      (handle! subject {:type :sim-next})
      (count (-> @fake-ui :convo :message-list)) => 10000)))