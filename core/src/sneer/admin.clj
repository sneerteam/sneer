(ns sneer.admin
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.core :as core]
   [sneer.impl :as impl]
   [sneer.networking.client :as client]
   [sneer.persistent-tuple-base :as persistence])
  (:import
   [sneer PrivateKey]
   [sneer.admin SneerAdmin]
   [sneer.impl.keys KeysImpl]
   [rx.subjects ReplaySubject PublishSubject]))

(defprotocol Restartable
  (restart [this]))

(defn new-sneer-admin

  ([own-prik network]
     (new-sneer-admin own-prik network (ReplaySubject/create)))

  ([^PrivateKey own-prik network tuple-base]
     (let [puk (.publicKey own-prik)
           connection (core/connect network puk)
           followees (PublishSubject/create)
           tuple-space (core/reify-tuple-space puk tuple-base connection followees)
           sneer (impl/new-sneer tuple-space own-prik followees)]
       (reify
         SneerAdmin
           (sneer [this] sneer)
           (privateKey [this] own-prik)
           (keys [this] this)
         sneer.keys.Keys
           (createPublicKey [this bytes-as-string]
             (throw (java.lang.RuntimeException.))  ; This seems not to be used.
             (KeysImpl/createPublicKey bytes-as-string))
         Restartable
           (restart [this]
             (rx/on-completed connection)
             (new-sneer-admin own-prik network (core/restarted tuple-base)))))))

(defn- produce-private-key [db]
  (if-let [existing (second (persistence/db-query db ["SELECT * FROM keys"]))]
    (KeysImpl/createPrivateKey ^bytes (first existing))
    (let [new-key (KeysImpl/createPrivateKey)]
      (persistence/db-insert db :keys {"prik" (.bytes new-key)})
      new-key)))

(defn new-sneer-admin-over-db [network db]
  (let [tuple-base (persistence/create db)
        own-prik (produce-private-key db)]
    (new-sneer-admin own-prik network tuple-base)))

(defn create [db]
  (new-sneer-admin-over-db (client/create-network) db))

