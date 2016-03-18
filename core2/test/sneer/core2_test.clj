(ns sneer.core2-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.test-util :refer :all]
    [sneer.core2 :refer :all]])

(facts "Contacts"
  (let [ui (atom nil)
        subject (sneer #(reset! ui %))]

    @ui => {:convo-list []}

    (fact "New contact appears on convo list"
      (handle! subject {:type :contact-new
                        :nick "Carla"})
      @ui => {:convo-list [{:nick "Carla"}]})))