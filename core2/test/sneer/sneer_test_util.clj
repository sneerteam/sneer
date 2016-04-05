(ns sneer.sneer-test-util
  (:require
   [sneer.core2 :refer :all]
   [sneer.streem :refer [transient-streems]]))

(defn- dummy-key [prefix key-type]
  (str (subs prefix 0 3) "-" key-type))

(def ^:private dummy-random (atom 1234))

(defn dummy-crypto-fns [own-name]
  {:generate-key-pair     #(do {"prik" (dummy-key own-name "prik")
                                "puk"  (dummy-key own-name "puk")})
   :generate-random-bytes #(do (swap! dummy-random inc)
                               (byte-array (take % (repeat @dummy-random))))})

(defn sneer-community []
  (atom nil))

(defn- server> [community packet]  ;packet {:from own-puk :send tuple :to puk}
  ()
  )

(defn- sneer-with-name [own-name ui-fn server>]
  (doto
    (sneer (transient-streems) ui-fn server> (dummy-crypto-fns own-name))
    (handle! {:type :own-name-set, :own-name own-name})))

(defn join [community own-name ui-fn]
  (let [server> (partial server> community)
        member (sneer-with-name own-name ui-fn server>)]
    (swap! community assoc (puk member) member)
    member))

(defn sneer-local [ui-fn]
  (let [server> nil]
    (sneer-with-name "Neide da Silva" ui-fn server>)))
