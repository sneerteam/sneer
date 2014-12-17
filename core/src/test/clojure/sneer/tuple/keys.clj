(ns sneer.tuple.keys
  (:import [sneer.crypto.impl KeysImpl]))

(def ^:private keys-impl (KeysImpl.))

(defn create-puk [^bytes rep]
  (.createPublicKey keys-impl rep))
