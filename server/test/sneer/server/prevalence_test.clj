(ns sneer.server.prevalence-test
  (:require
    [sneer.server.prevalence :refer :all]
    [midje.sweet :refer :all]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- tmp-file []
  (doto 
    (java.io.File/createTempFile "test-" ".tmp")
    (.delete)))

#_(fact "Prevalence"
  (let [handler +
        initial-state 0
        file (tmp-file)
        
        prevayler! #(prevayler-jr! handler initial-state file)]
    
    (let [p1 (prevayler!)]
      @p1 => 0
      (handle! p1 42)
      @p1 => 42
      (handle! p1 100)
      @p1 => 142
      (close! p1))
    
    ; Restart with same file (initial state saved as first item)
    (let [p2 (prevayler!)]
      @p2 => 142
      (handle! p2 1000)
      @p2 => 1142
      (close! p2))

    ; Restart with same file (previous state saved as first item)
    (let [p3 (prevayler!)]
      @p3 => 1142
      (handle! p3 10000)
      @p3 => 11142
      (close! p3))
    
    ; Simulate crash during log rolling.
    (assert (.renameTo file (replacement file)))
    (let [p4 (prevayler!)]
      @p4 => 11142
      (close! p4))))
