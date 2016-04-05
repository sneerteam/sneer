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

(defn- outbox-fn [network-sim sneer-atom event-out]  ;packet {:from own-puk :send tuple :to puk}
  (let [puk (puk @sneer-atom)]
    (net/send-packet network-sim {:from puk
                                  :to   (event-out :to)
                                  :send (assoc (event-out :event) :from puk)}))
  (handle! @sneer-atom {:type :event-sent, :event event-out}))

(defn- sneer-with-name [own-name ui-fn outbox-fn]
  (doto
    (sneer (transient-streems) ui-fn outbox-fn (dummy-crypto-fns own-name))
    (handle! {:type :own-name-set, :own-name own-name})))

(defn- handle-packet [member packet]
  (handle! member {:type  :event-in
                   :event (packet :send)}))

(defn join [network-sim own-name ui-fn]
  (let [sneer (atom nil)
        outbox-fn (partial outbox-fn network-sim sneer)]
    (reset! sneer (sneer-with-name own-name ui-fn outbox-fn))
    (net/join network-sim (puk @sneer) (partial handle-packet @sneer))
    @sneer))

(defn sneer-local [ui-fn]
  (let [outbox-fn nil]
    (sneer-with-name "Neide da Silva" ui-fn outbox-fn)))
