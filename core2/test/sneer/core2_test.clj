(ns sneer.core2-test
  (:use midje.sweet)
  (:use [sneer.core2-sim]))

(facts "UI Sims"
  (let [ui (atom nil)
        subject (sneer #(reset! ui %))]

    (fact "Convo sims are generated"
      (handle! subject {"event" "sim-next"})
      @ui => {"view" "convos"
              "convo-list" []}

      (handle! subject {"event" "sim-next"})
      @ui => {"view" "convos"
              "convo-list" [{"id"       1000
                             "nickname" "Neide 0"
                             "preview"  "Hi There! 0"
                             "date"     "Today 0"
                             "unread"   ""}]}

      (handle! subject {"event" "sim-next"})
      (count (@ui "convo-list")) => 100)


    (fact "Message sims are generated"
      (handle! subject {"event" "sim-next"})
      @ui => {"view" "convo"
              "id" 1042
              "tab" "chat"
              "message-list" []}

      (handle! subject {"event" "sim-next"})
      @ui => {"view" "convo"
              "id" 1042
              "tab" "chat"
              "message-list" [{"id"     10000
                               "is-own" true
                               "text"   "Hi There! 0"
                               "date"     "Today 0"}]}

      (handle! subject {"event" "sim-next"})
      (count (@ui "message-list")) => 10000)))