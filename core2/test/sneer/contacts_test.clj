(ns sneer.contacts-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.core2 :refer :all]
    [sneer.sneer-test-util :refer :all]])

(facts "Contacts"
  (let [ui (atom nil)
        subject (sneer-local #(reset! ui %))]

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
        (get-in @ui [:convo-list 0 :nick]) => "Kharla"))

    (fact "Nicks with problems are ignored. The ui should never actually trigger this event because it can validate nicks first."
      (handle! subject {:type :contact-new
                        :nick "Kharla"})
      (-> @ui :convo-list count) => 1)

    (fact "Problems with new nickname can be validated"
      (handle! subject {:type          :view
                        :nick-validation "Maicon"})
      (-> @ui :nick-validation :nick)    => "Maicon"
      (-> @ui :nick-validation :problem) => nil

      (handle! subject {:type          :view
                        :nick-validation ""})
      (-> @ui :nick-validation :nick)    => ""
      (-> @ui :nick-validation :problem) => "cannot be empty"

      (handle! subject {:type          :view
                        :nick-validation "Kharla"})
      (-> @ui :nick-validation :nick)    => "Kharla"
      (-> @ui :nick-validation :problem) => "already used")))
