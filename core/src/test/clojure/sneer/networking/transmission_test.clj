(ns sneer.networking.transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.networking.transmission :refer [start-transciever new-retry-timeout]]
            [sneer.async :refer :all]
            [sneer.test-util :refer :all]
            [clojure.core.async :as async :refer [chan >!! <! go-loop close!]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts
 "About value transmission"

 (let [data (fn [d] [d d d])
       hash-fn (fn [data] (data 0))
       
       to-b   (chan 10)
       raw-from-b (chan 10)
       from-b (distinct-until-changed< raw-from-b)
       lease-a (chan)

       to-a   (chan 10)
       raw-from-a (chan 10)
       from-a (distinct-until-changed< raw-from-a)
       lease-b (chan)

       packets-ab (chan 1)
       packets-ba (chan 1)
        
       a (start-transciever to-b raw-from-b packets-ab packets-ba hash-fn lease-a)
       b (start-transciever to-a raw-from-a packets-ba packets-ab hash-fn lease-b)]

   (with-redefs [new-retry-timeout (constantly IMMEDIATELY)]
    
     (fact
      "One value is transmitted from A to B"
      (>!! to-b (data 0))
      (<!!? from-a) => (data 0))

     (fact
     "Another value is transmitted from A to B"
     (>!! to-b (data 42))
     (<!!? from-a) => (data 42))

     (fact
       "One value is transmitted from B to A"
       (>!! to-a (data 100))
       (<!!? from-b) => (data 100))

     (fact
      "Several values are transmitted in both directions"
      (>!! to-b (data   1))
      (>!! to-a (data 101))
      (>!! to-b (data   2))
      (>!! to-a (data 102))
      (>!! to-a (data 103))
      (>!! to-b (data   3))
      (<!!? from-a) => (data   1)
      (<!!? from-a) => (data   2)
      (<!!? from-a) => (data   3)
      (<!!? from-b) => (data 101)
      (<!!? from-b) => (data 102)
      (<!!? from-b) => (data 103))

     (fact
      "Transceivers close when lease is closed"
      (close! lease-a)
      (<!!? (async/filter< nil? a)) => nil
      (close! lease-b)
      (<!!? (async/filter< nil? b)) => nil)
     
     #_(fact
        "Retry IMMEDIATELY"
        "Retry" => "IMMEDIATELY"))))
