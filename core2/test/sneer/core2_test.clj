(ns sneer.core2-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.test-util :refer :all]
    [sneer.core2 :refer :all]])

(facts "Contacts"
  (let [ui (atom nil)
        streems "To be implemented"
        subject (sneer #(reset! ui %) streems)]

    @ui => {:convo-list []}

    (fact "New contact appears on convo list"
      (handle! subject {:type :contact-new
                        :nick "Carla"})

      (get-in @ui [:convo-list 0 :nick]) => "Carla"

      (handle! subject {:type :contact-new
                        :nick "Maico"})

      (get-in @ui [:convo-list 1 :nick]) => "Maico")

    (fact "Deleted contact is removed from convo list"
      (let [carla-id (get-in @ui [:convo-list 0 :contact-id])]
        carla-id => some?

        (handle! subject {:type :contact-delete
                          :contact-id carla-id})

        @ui => {:convo-list [{:contact-id 1, :nick "Maico"}]}))))
