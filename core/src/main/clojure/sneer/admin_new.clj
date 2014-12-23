(ns sneer.admin-new
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.tuple.space :as space]
   [sneer.impl-new :as impl]
   [sneer.networking.client-new :as client]
   [sneer.tuple.persistent-tuple-base :as persistence])
  (:import
   [sneer PrivateKey]
   [sneer.admin SneerAdmin]
   [sneer.crypto.impl KeysImpl]
   [rx.subjects ReplaySubject PublishSubject]))

(defprotocol Restartable
  (restart [this]))

(defn connect [client puk]
  (assert false))

(defn new-sneer-admin
  [^PrivateKey own-prik client tuple-base]
  (let [puk (.publicKey own-prik)
        tuple-space (space/reify-tuple-space puk tuple-base)
        sneer (impl/new-sneer tuple-space own-prik)]
    (reify
      SneerAdmin
      (sneer [this] sneer)
      (privateKey [this] own-prik)
      (keys [this] (KeysImpl.)))))

(defn- produce-private-key [db]
  (if-let [existing (second (persistence/db-query db ["SELECT * FROM keys"]))]
    (.createPrivateKey (KeysImpl.) ^bytes (first existing))
    (let [new-key (.createPrivateKey (KeysImpl.))]
      (persistence/db-insert db :keys {"prik" (.toBytes new-key)})
      new-key)))

(defn new-sneer-admin-over-db [db client]
  (let [tuple-base (persistence/create db)
        own-prik (produce-private-key db)]
    (new-sneer-admin own-prik client tuple-base)))
