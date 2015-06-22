(ns sneer.async-test
  (:require [clojure.core.async :refer [chan close!]]
            [midje.sweet :refer :all]
            [sneer.async :refer [state-machine sliding-chan go-trace]]
            [sneer.test-util :refer [<!!? >!!? closes]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(fact "State machine works"
  (let [events (chan)
        function +
        initial-state 42
        machine (state-machine initial-state function events)
        tap1 (sliding-chan)
        tap2 (sliding-chan)
        tap3 (sliding-chan)]
    (>!!? machine tap1)
    (<!!? tap1) => 42

    (>!!? events 200)
    (<!!? tap1) => 242

    (>!!? machine tap2)
    (>!!? events 4000)
    (<!!? tap1) => 4242
    (<!!? tap2) => 4242

    (close! tap1)
    (>!!? machine tap3)
    (>!!? events 20000)
    (<!!? tap2) => 24242
    (<!!? tap3) => 24242

    (close! events)
    tap2 => closes
    tap3 => closes))
