(ns sneer.keys
  (:import [sneer.crypto.impl KeysImpl]))

(def ^:private keys-impl (KeysImpl.))

(defn create-prik []
  (.createPrivateKey keys-impl))

(defn create-puk [^bytes rep]
  (.createPublicKey keys-impl rep))

(defn from-hex [^String hex]
  (.createPublicKey keys-impl hex))

(defn ->puk [^String rep]
  (create-puk (.getBytes rep)))
