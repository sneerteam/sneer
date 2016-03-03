(ns sneer.core2-test
  (:use midje.sweet)
  (:use [sneer.core2]))

(facts "foo"
  (foo) => "bar")
