(ns sneer.server.prevalence-test
  (:require
    [sneer.server.prevalence :refer :all]
    [midje.sweet :refer :all]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- tmp-file []
  (doto 
    (java.io.File/createTempFile "test-" ".tmp")
    (.delete)))

(fact "Prevalence"
  (let [file (tmp-file)
        initial-state 0
        handler +]
    
    (let [p1 (prevayler-jr! file initial-state handler)]
      (state p1) => 0
      (handle! p1 42)
      (state p1) => 42
      (handle! p1 100)
      (state p1) => 142
      (close! p1))
    
    ; Restart with same file (initial state saved as first item)
    (let [p2 (prevayler-jr! file initial-state handler)]
      (state p2) => 142
      (handle! p2 1000)
      (state p2) => 1142
      (close! p2))

    ; Restart with same file (previous state saved as first item)
    (let [p3 (prevayler-jr! file initial-state handler)]
      (state p3) => 1142
      (handle! p3 10000)
      (state p3) => 11142
      (close! p3))
    
    ; Simulate crash during log rolling.
    (assert (.renameTo file (replacement file)))
    (let [p4 (prevayler-jr! file initial-state handler)]
      (state p4) => 11142
      (close! p4))))
