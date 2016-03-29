(ns sneer.chat-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.core2 :refer :all]
    [sneer.streem :refer :all]])

(facts "Chat"
  (let [ui (atom nil)
        streems (streems)
        subject (sneer #(reset! ui %) streems)]

    (handle! subject {:type :contact-new
                      :nick "Carla"})

    (let [carla-id (get-in @ui [:convo-list 0 :contact-id])]

      (fact "Empty chat can be viewed"
        (handle! subject {:type :view
                          :path [:convo carla-id]})
        (get-in @ui [:convo :contact-id]) => carla-id
        (get-in @ui [:convo :nick]) => "Carla"
        (get-in @ui [:convo :chat]) => [])

      #_(fact "Sent message appears in chat"
        (handle! subject {:type :message-send
                          :contact-id carla-id
                          :text "Hi"})
        (get-in @ui [:convo :chat 0 :text]) => "Hi"
        (handle! subject {:type :message-send
                          :contact-id carla-id
                          :text "How are you?"})
        (get-in @ui [:convo :chat 1 :text]) => "How are you?"
        ))

    ))
