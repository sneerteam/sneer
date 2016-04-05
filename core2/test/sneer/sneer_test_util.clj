(ns sneer.sneer-test-util
  (:require
   [sneer.core2 :refer :all :as core]
   [sneer.network-sim :as net]
   [sneer.streem :refer [transient-streems]]))

(defn- dummy-key [prefix key-type]
  (str (subs prefix 0 3) "-" key-type))

(def ^:private dummy-random (atom 1234))

(defn dummy-crypto-fns [own-name]
  {:generate-key-pair     #(do {"prik" (dummy-key own-name "prik")
                                "puk"  (dummy-key own-name "puk")})
   :generate-random-bytes #(do (swap! dummy-random inc)
                               (byte-array (take % (repeat @dummy-random))))})

(defn- outbox-fn [network-sim from event-out]  ;packet {:from own-puk :send tuple :to puk}
  (net/send-packet network-sim {:to   (:to event-out)
                                :send (:event event-out)}))

(defn- sneer-with-name [own-name ui-fn outbox-fn]
  (doto
    (sneer (transient-streems) ui-fn outbox-fn (dummy-crypto-fns own-name))
    (handle! {:type :own-name-set, :own-name own-name})))

(defn- handle-packet [member packet]
  (handle! member {:type  :event-in
                   :event (packet :send)}))

(defn join [network-sim own-name ui-fn]
  (let [puk (atom nil)
        outbox-fn (partial outbox-fn network-sim @puk)
        member (sneer-with-name own-name ui-fn outbox-fn)]
    (reset! puk (core/puk member))
    (net/join network-sim @puk (partial handle-packet member))
    member))

(defn sneer-local [ui-fn]
  (let [outbox-fn nil]
    (sneer-with-name "Neide da Silva" ui-fn outbox-fn)))
