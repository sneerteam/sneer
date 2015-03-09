(ns sneer.admin
  (:require
   [sneer.tuple.space :as space]
   [sneer.impl :as impl]
   [sneer.tuple.protocols :refer :all]
   [sneer.tuple.persistent-tuple-base :as persistence]
   [sneer.restartable :refer :all]
   [sneer.tuple-base-provider :refer :all])
  (:import
    [sneer PrivateKey]
    [sneer.admin SneerAdmin]
    [sneer.crypto.impl KeysImpl]))

(defn new-sneer-admin
  [^PrivateKey own-prik tuple-base]
  (let [puk (.publicKey own-prik)
        tuple-space (space/reify-tuple-space puk tuple-base)
        sneer (impl/new-sneer tuple-space own-prik)]
    (reify
      SneerAdmin
      (sneer [this] sneer)
      (privateKey [this] own-prik)
      (keys [this] (KeysImpl.))

      TupleBaseProvider
      (tuple-base-of [this] tuple-base)

      Restartable
      (restart [this]
        (new-sneer-admin own-prik (restarted tuple-base))))))

(defn- produce-private-key [db]
  (if-let [existing (second (db-query db ["SELECT * FROM keys"]))]
    (.createPrivateKey (KeysImpl.) ^bytes (first existing))
    (let [new-key (.createPrivateKey (KeysImpl.))]
      (db-insert db :keys {"prik" (.toBytes new-key)})
      new-key)))

(defn new-sneer-admin-over-db [db]
  (let [tuple-base (persistence/create db)
        own-prik (produce-private-key db)]
    (new-sneer-admin own-prik tuple-base)))
