(ns sneer.sneer-test-util
  (:require
   [sneer.core2 :refer :all]
   [sneer.streem :refer :all]))

(def ^:private crypto-fns
  {:generate-key-pair #(do {"prik" "foo-prik",
                            "puk"  "foo-puk"})})

(defn sneer-local [ui-fn]
  (sneer ui-fn (streems) nil crypto-fns))
