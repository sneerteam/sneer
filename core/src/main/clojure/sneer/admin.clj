(ns sneer.admin
  (:require
    [sneer.commons :refer [dispose]]
    [sneer.tuple.space :as space]
    [sneer.impl :as impl]
    [sneer.tuple.protocols :refer :all]
    [sneer.tuple.persistent-tuple-base :as persistence]
    [sneer.restartable :refer :all]
    [sneer.tuple-base-provider :refer :all]
    [sneer.async :refer [go-while-let]]
    [rx.lang.clojure.core :as rx]
    [clojure.core.async :refer [chan <!]])
  (:import
    [java.lang AutoCloseable]
    [sneer PrivateKey]
    [sneer.admin SneerAdmin]
    [sneer.crypto.impl KeysImpl]))

(defn handle-invites [sneer tuple-base puk]
  (let [tuples-out (chan)
        lease (chan)]
    (query-tuples tuple-base {"type" "push" "audience" puk} tuples-out lease)
    (go-while-let [tuple (<! tuples-out)]
      (when-some [invite-code (get tuple "invite-code")]
        (rx/subscribe (.. sneer contacts first)
                      (fn [contacts]
                        (some-> (first (filter #(= (.inviteCode %) invite-code) contacts))
                                (.setParty (.produceParty sneer (get tuple "author"))))))))))

(defn new-sneer-admin
  [^PrivateKey own-prik tuple-base]
  (let [puk (.publicKey own-prik)
        tuple-space (space/reify-tuple-space puk tuple-base)
        sneer (impl/new-sneer tuple-space own-prik)]
    (handle-invites sneer tuple-base puk)
    (reify
      SneerAdmin
      (sneer [_] sneer)
      (privateKey [_] own-prik)
      (keys [_] (KeysImpl.))

      TupleBaseProvider
      (tuple-base-of [_] tuple-base)

      Restartable
      (restart [_]
        (new-sneer-admin own-prik (restarted tuple-base)))

      AutoCloseable
      (close [_]
        (dispose tuple-base)))))

(defn- produce-private-key [db]
  (if-let [existing (second (db-query db ["SELECT * FROM keys"]))]
    (.createPrivateKey (KeysImpl.) ^bytes (first existing))
    (let [new-key (.createPrivateKey (KeysImpl.))]
      (db-insert db :keys {"prik" (.toBytes new-key)})
      new-key)))

(defn- ensure-protocol [db]
  (if (instance? sneer.admin.Database db)
    (let [^sneer.admin.Database db db]
      (reify Database
        (db-create-table [_ table columns]
          (.createTable db (name table) columns))
        (db-create-index [_ table index-name column-names unique?]
          (.createIndex db (name table) (name index-name) (mapv name column-names) unique?))
        (db-insert [_ table row]
          (.insert db (name table) row))
        (db-query [_ sql-and-params]
          (.query db (first sql-and-params) (subvec sql-and-params 1)))))
    db))

(defn new-sneer-admin-over-db [db]
  (let [db (ensure-protocol db)
        tuple-base (persistence/create db)
        own-prik (produce-private-key db)]
    (new-sneer-admin own-prik tuple-base)))
