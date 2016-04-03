(ns sneer.sneer-test-util
  (:require
   [sneer.core2 :refer :all]
   [sneer.streem :refer :all]))

(defn- dummy-key [prefix key-type]
  (str (subs prefix 0 3) "-" key-type))

(def ^:private dummy-random (atom 1234))

(defn dummy-crypto-fns [own-name]
  {:generate-key-pair #(do {"prik" (dummy-key own-name "prik")
                            "puk"  (dummy-key own-name "puk")})
   :random-long-generator #(swap! dummy-random inc)})

(defn sneer-local [ui-fn]
  (sneer ui-fn (streems) nil (dummy-crypto-fns "Neide")))
