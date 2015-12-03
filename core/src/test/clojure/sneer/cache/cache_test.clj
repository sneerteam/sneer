(ns sneer.cache.cache-test
  (:require [clojure.core.cache :as cache]
            [midje.sweet :refer [fact facts]]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts "About cache"
  (fact "core.cache works"
    (let [C (cache/fifo-cache-factory {:a 1, :b 2})]
      C => {:a 1, :b 2}

      (cache/miss C :c 42) => {:a 1, :b 2, :c 42}

      (cache/evict C :b) => {:a 1}
      )))
