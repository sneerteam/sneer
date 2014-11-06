(ns sneer.networking.transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.networking.transmission :refer [start-transciever new-retry-timeout]]
            [sneer.async :refer :all]
            [sneer.test-util :refer :all]
            [clojure.core.async :as async :refer [chan >!! <! go-loop close!]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts
 "About value transmission"

 (let [arr (fn [b] (doto (byte-array 100) (java.util.Arrays/fill (byte b))))
       arr? (fn [b] (fn [actual] (= (seq (arr b)) (seq actual))))
       hash-fn (fn [bytes] (aget bytes 0))
       
       to-b   (chan 10)
       from-b (chan 10)
       lease-a (chan)

       to-a   (chan 10)
       from-a (chan 10)
       lease-b (chan)

       packets-ab (chan 1)
       packets-ba (chan 1)
        
       a (start-transciever to-b from-b packets-ab packets-ba hash-fn lease-a)
       b (start-transciever to-a from-a packets-ba packets-ab hash-fn lease-b)]

   (with-redefs [new-retry-timeout (constantly (chan))]
    
     (fact
      "One value is transmitted from A to B"
      (>!! to-b (arr 0))
      (<!!? from-a) => (arr? 0))

     (fact
     "Another value is transmitted from A to B"
     (>!! to-b (arr 42))
     (<!!? from-a) => (arr? 42))

     (fact
       "One value is transmitted from B to A"
       (>!! to-a (arr 100))
       (<!!? from-b) => (arr? 100))

     (fact
      "Several values are transmitted in both directions"
      (>!! to-b (arr   1))
      (>!! to-a (arr 101))
      (>!! to-b (arr   2))
      (>!! to-a (arr 102))
      (>!! to-a (arr 103))
      (>!! to-b (arr   3))
      (<!!? from-a) => (arr?   1)
      (<!!? from-a) => (arr?   2)
      (<!!? from-a) => (arr?   3)
      (<!!? from-b) => (arr? 101)
      (<!!? from-b) => (arr? 102)
      (<!!? from-b) => (arr? 103))

     (fact
      "Transceivers close when lease is closed"
      (close! lease-a)
      (<!!? (async/filter< nil? a)) => nil
      (close! lease-b)
      (<!!? (async/filter< nil? b)) => nil)
     
     #_(fact
        "Retry IMMEDIATELY"
        "Retry" => "IMMEDIATELY"))))
