(ns sneer.core2-sim-test
  (:use midje.sweet)
  (:use [sneer.core2-sim]))

(facts "UI Sims"
  (let [fake-ui (atom nil)
        dispatcher (dispatcher #(reset! fake-ui %))]

    (fact "Convo sims are generated"
      (+ 1 1) => 2

      (dispatch! dispatcher {"event" "sim-next"})
      @fake-ui => {"view" "convos"
              "convo-list" []}

      (dispatch! dispatcher {"event" "sim-next"})
      @fake-ui => {"view" "convos"
              "convo-list" [{"id"       1000
                             "nickname" "Neide 0"
                             "preview"  "Hi There! 0"
                             "date"     "Today 0"
                             "unread"   ""}]}

      (dispatch! dispatcher {"event" "sim-next"})
      (count (@fake-ui "convo-list")) => 100)


    (fact "Message sims are generated"
      (dispatch! dispatcher {"event" "sim-next"})
      @fake-ui => {"view" "convo"
              "id" 1042
              "tab" "chat"
              "message-list" []}

      (dispatch! dispatcher {"event" "sim-next"})
      @fake-ui => {"view" "convo"
              "id" 1042
              "tab" "chat"
              "message-list" [{"id"     10000
                               "is-own" true
                               "text"   "Hi There! 0"
                               "date"   "Today 0"}]}

      (dispatch! dispatcher {"event" "sim-next"})
      (count (@fake-ui "message-list")) => 10000)

    (fact "Events are simulated as toasts"
      (dispatch! dispatcher {"event" "select-convo"
                             "id" 1042})
      (@fake-ui "toast") => "{\"event\" \"select-convo\", \"id\" 1042}")))