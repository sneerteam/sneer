(ns mvstore.mvstore-test
  (:require [midje.sweet :refer :all])
  (:import
    [org.h2.mvstore MVStore]
    (java.util.concurrent Semaphore)))

(facts "MVStore Works"
  (let [in-memory nil
        store (MVStore/open in-memory)
        map1 (.openMap store "map1")]

    (fact "Map put and get work."
      (.get map1 "key") => nil
      (.put map1 "key" "value")
      (.get map1 "key") => "value")

    (fact "Arrays work as keys"
      (.put map1 (.getBytes "key") "value")
      (.get map1 (.getBytes "key")) => "value")

    (fact "Serializable objects can be stored"
      (.put map1 "key" (Semaphore. 42))
      (-> map1 (.get "key") .availablePermits) => 42)))