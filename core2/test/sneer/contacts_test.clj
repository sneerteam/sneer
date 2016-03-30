(ns sneer.contacts-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.core2 :refer :all]
    [sneer.streem :refer :all]])

(facts "Contacts"
  (let [ui (atom nil)
        streems (streems)
        subject (sneer #(reset! ui %) streems)]

    (@ui :convo-list) => []

    (fact "New contact appears on top of convo list"
      (handle! subject {:type :contact-new
                        :nick "Carla"})
      (get-in @ui [:convo-list 0 :nick]) => "Carla"

      (handle! subject {:type :contact-new
                        :nick "Maico"})
      (get-in @ui [:convo-list 0 :nick]) => "Maico"
      (get-in @ui [:convo-list 1 :nick]) => "Carla")

    (fact "Deleted contact is removed from convo list"
      (let [maico-id (get-in @ui [:convo-list 0 :contact-id])]
        (handle! subject {:type :contact-delete
                          :contact-id maico-id})
        (get-in @ui [:convo-list 0 :nick]) => "Carla"))

    (fact "Contacts can be renamed"
      (let [carla-id (get-in @ui [:convo-list 0 :contact-id])]
        (handle! subject {:type :contact-rename
                          :contact-id carla-id
                          :new-nick "Kharla"})
        (get-in @ui [:convo-list 0 :nick]) => "Kharla"))))
