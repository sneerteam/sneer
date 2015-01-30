(ns sneer.keys
  (:import [sneer.crypto.impl KeysImpl]))

(let [keys-impl (KeysImpl.)]
  (defn create-puk [^bytes rep]
    (.createPublicKey keys-impl rep))

  (defn ->puk [^String rep]
    (create-puk (.getBytes rep))))
