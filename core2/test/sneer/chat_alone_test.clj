(ns sneer.chat-alone-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.core2 :refer :all]
    [sneer.sneer-test-util :refer :all]])

(facts "Chatting alone (before contact accepts invite)"
  (let [ui (atom nil)
        subject (sneer-local #(reset! ui %))]

    (handle! subject {:type :contact-new
                      :nick "Carla"})

    (let [carla-id (get-in @ui [:convo-list 0 :contact-id])]

      (fact "Empty chat can be viewed"
        (handle! subject {:type :view
                          :path [:convo carla-id]})
        (get-in @ui [:convo :contact-id]) => carla-id
        (get-in @ui [:convo :nick]) => "Carla"
        (get-in @ui [:convo :chat]) => [])

      (fact "Sent message appears in chat"
        (handle! subject {:type :msg-send
                          :contact-id carla-id
                          :text "Hi"})
        (get-in @ui [:convo :chat 0 :text]) => "Hi"

        (handle! subject {:type :msg-send
                          :contact-id carla-id
                          :text "How are you?"})
        (get-in @ui [:convo :chat 1 :text]) => "How are you?")

      (fact "Last sent message appears in convo-list summary preview"
        (get-in @ui [:convo-list 0 :preview]) => "How are you?"

        (handle! subject {:type :msg-send
                          :contact-id carla-id
                          :text "Answer me!!!"})
        (get-in @ui [:convo-list 0 :preview]) => "Answer me!!!"))))
