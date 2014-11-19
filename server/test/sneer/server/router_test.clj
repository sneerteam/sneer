(ns sneer.server.router-test
  (:require
   [midje.sweet :refer :all]
   [sneer.test-util :refer :all]
   [sneer.server.router :refer :all]))

(facts
  "Routing"

  (let [max-q-size 3
        subject (atom nil)
        ; DSL:
        restart! #(reset! subject (create-router max-q-size))
        enq! (fn [from to msg] (-> subject (swap! enqueue! from to msg)
                                 (queue-full? from to)
                                 not))
        peek #(peek-tuple-for @subject %)
        pop? #(let [original @subject
                    router (swap! subject pop-tuple-for %)]
                (sender-to-notify original router %))
        pop! #(do (pop? %) (peek %))
        ]
    
    (restart!)
    (fact "Queues start empty and accept tuples."
      (peek :B) => nil
      (enq! :A :B "Hello") => true)

    (fact "One value is routed from A to B"
      (peek :B) => "Hello")

    (fact "One value is routed from B to A"
      (enq! :B :A "Hi there")
      (peek :A) => "Hi there")

    (fact "Tuples are enqueued."
      (enq! :A :B "Hello Again")
      (peek :B) => "Hello"
      (pop! :B) => "Hello Again")

    (fact "Queues grow up to max-size."
      (enq! :A :B "Msg 2") => true
      (enq! :A :B "Msg 3") => false
      (enq! :A :B "Msg 4") => false)
    
    (fact "Tuples from multiple senders are multiplexed."
      (enq! :C :B "Hello  from C")
      (enq! :C :B "Hello2 from C")
      (pop! :B) => "Hello  from C"
      (pop! :B) => "Msg 2"
      (pop! :B) => "Hello2 from C"
      (pop! :B) => "Msg 3")
  
    (restart!)
    (fact "Blank crazy pop doesn't crash"
      (pop! :Foo) => nil)

    (restart!)
    (fact "Queues that become empty return nil."
      (enq! :A :B "A1")
      (enq! :C :B "C1")
      (enq! :C :B "C2")
      (peek :B) => "A1"
      (pop! :B) => "C1"
      (pop! :B) => "C2"
      (pop! :B) => nil)

    (restart!)
    (fact "Multiple receivers can have enqueued tuples."
      (enq! :A :B "AB1")
      (enq! :A :C "AC1")
      (enq! :B :A "BA1")
      (enq! :B :C "BC1")
      (peek :A) => "BA1"
      (peek :B) => "AB1"
      (peek :C) => "AC1"
      (pop! :A) => nil
      (pop! :B) => nil
      (pop! :C) => "BC1"
      (pop! :C) => nil)
    
    (restart!)
    (fact "Senders are notified of queues that were full and became empty."
      (enq! :A :B "AB1")
      (enq! :A :B "AB2")
      (enq! :A :B "AB3")
      (enq! :A :B "AB4") => false
      (enq! :C :B "CB1")
      (enq! :C :B "CB2")
      (enq! :C :B "CB3")
      (enq! :C :B "CB4") => false
      (pop? :B) => nil
      (pop? :B) => nil
      (pop? :B) => nil
      (pop? :B) => nil
      (pop? :B) => :A
      (pop? :B) => :C
      (pop? :B) => nil)))
