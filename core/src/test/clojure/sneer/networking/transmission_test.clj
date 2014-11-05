(ns sneer.networking.transmission-test
  (:require [midje.sweet :refer :all]
            [sneer.networking.transmission :refer [start-transciever new-retry-timeout]]
            [sneer.async :refer :all]
            [sneer.test-util :refer :all]
            [clojure.core.async :as async :refer [chan >!! <! go-loop close!]]))

; (do (require 'midje.repl) (midje.repl/autotest))

#_(facts
   "About value transmission"

   (let [tuples-a (mapv #(do {:id % :some-field (str "Value A " %)}) (range 0 4))
         tuples-b (mapv #(do {:id % :some-field (str "Value B " %)}) (range 0 4))
        
         to-b   (chan 10)
         from-b (chan 10)
         lease-a (chan)

         to-a   (chan 10)
         from-a (chan 10)
         lease-b (chan)

         packets-ab (chan)
         packets-ba (chan)
        
         a (start-transciever to-b from-b packets-ab packets-ba lease-a)
         b (start-transciever to-a from-a packets-ba packets-ab lease-b)]

     (with-redefs [new-retry-timeout (constantly IMMEDIATELY)]
    
       (fact
         "One value is transmitted from A to B"
         (>!! to-b (tuples-a 0))
         (<!!? from-a) => (tuples-a 0))

       (fact
         "One value is transmitted from B to A"
         (>!! to-a :b1)
         (<!!? from-b) => :b1)

       (fact
         "Several values are transmitted in both directions"
         (>!! to-b (tuples-a 1))
         (>!! to-a (tuples-b 1))
         (>!! to-b (tuples-a 2))
         (>!! to-a (tuples-b 2))
         (>!! to-a (tuples-b 3))
         (>!! to-b (tuples-a 3))
         (doseq [ta (rest tuples-a)] (<!!? from-a) => ta)
         (doseq [tb (rest tuples-b)] (<!!? from-b) => tb))

       (fact
         "Transceivers close when lease is closed"
         (close! lease-a)
         (<!!? (async/filter< nil? a)) => nil
         (close! lease-b)
         (<!!? (async/filter< nil? b)) => nil))))