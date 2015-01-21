(ns sneer.server.prevalence-test
  (:require
    [sneer.server.prevalence :refer :all]
    [midje.sweet :refer :all]
    [clojure.core.async :as async :refer [thread to-chan chan close! >!!]]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "Logging"
  (fact "Empty log"
    (let [file (java.io.File/createTempFile "test-" "-tmp")
          replay (chan)
          log (chan)]
      (logger file replay log)
      (<!!? replay) => nil))

  (fact "Items in the log"
    (let [file (java.io.File/createTempFile "test-" "-tmp")]
      (let [replay (chan)
            log (chan)]
        (logger file replay log)
        (<!!? replay) => nil
        (>!!? log :a)
        (>!!? log :b)
        (>!!? log :c)
        (close! log))
      (let [replay (chan)
            log (chan)]
        (logger file replay log)
        (<!!? (async/into [] replay)) => [:a :b :c]
        (close! log))))
  
)

