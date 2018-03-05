(ns sneer.server.router-priority-test
  (:require
    [midje.sweet :refer [fact facts]]
    [sneer.test-util :refer :all]
    [sneer.server.router-test :refer [router-test-dsl]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(def priority1 1)
(def priority2 2)
(def priority3 3)

(facts
  "Priority Routing"

  (let [{:keys [restart! enq! peek pop!]} (router-test-dsl)]
    
    (restart!)
    (fact "Routing with same priority delivers all tuples. Priority 1 is default."
      (enq! :A :B "Hello1" priority1) => true
      (enq! :A :B "Hello2")           => true
      (enq! :A :B "Hello3" priority1) => false
      (peek :B) => "Hello1"
      (pop! :B) => "Hello2"
      (pop! :B) => "Hello3"
      (pop! :B) => nil)

    #_(fact "Tuple routing is prioritized"
      (enq! :A :B "Hello3" priority3) => true
      (enq! :A :B "Hello1" priority1)           => true
      (enq! :A :B "Hello2" priority2) => false
      (peek :B) => "Hello1"
      (pop! :B) => "Hello2"
      (pop! :B) => "Hello3"
      (pop! :B) => nil)))
