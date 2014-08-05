(ns sneer.core-test
  (:require
   [clojure.core.async :as async :refer [chan >! <! >!! <!! alts!! timeout]]
   [clojure.test :refer :all]
   [midje.sweet :refer :all]
   [sneer.server.core :as core]
   [sneer.server.io :as io]))

(defn <?!! [c]
  (let [[v _] (alts!! [c (timeout 100)])]
    v))

(facts "Facts")
