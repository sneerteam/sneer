(ns sneer.chat-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.test-util :refer :all]
    [sneer.core2 :refer :all]
    [sneer.streem :refer :all]])

(facts "Chat"
  (let [ui (atom nil)
        streems (streems)
        subject (sneer #(reset! ui %) streems)]

    (handle! subject {:type :contact-new
                      :nick "Carla"})

    (fact "Chat can be viewed"

      (let [carla-id (get-in @ui [:convo-list 0 :contact-id])]
        (handle! subject {:type :view
                          :path [:convo carla-id]}))

      (get-in @ui [:convo :nick]) => "Carla"
      (get-in @ui [:convo :chat]) => []

      )

    ))
