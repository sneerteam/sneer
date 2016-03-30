(ns sneer.own-name-test
  (:require
   [midje.sweet :refer [facts fact]]
   [sneer.midje-util :refer :all]
   [sneer.core2 :refer :all]
   [sneer.streem :refer :all]))

(facts "Own name"
  (let [ui (atom nil)
        streems (streems)
        subject (sneer #(reset! ui %) streems)]

    (fact "She has a name"
      (get-in @ui [:profile :own-name]) => nil

      (handle! subject {:type :own-name-set
                        :own-name "Neide da Silva"})

      (get-in @ui [:profile :own-name]) => "Neide da Silva")))
