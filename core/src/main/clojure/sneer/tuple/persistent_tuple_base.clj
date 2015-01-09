(ns sneer.tuple.persistent-tuple-base
  (:import [sneer.crypto.impl KeysImpl])
  (:require [sneer.core :as core]
            [sneer.async :refer [dropping-chan go-trace dropping-tap]]
            [clojure.core.async :as async :refer [go-loop <! >! >!! mult tap chan close! go]]
            [sneer.rx :refer [filter-by seq->observable]]
            [clojure.core.match :refer [match]]
            [sneer.serialization :as serialization]
            [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.interop :as rx-interop]))

(defprotocol TupleBase
  "A backing store for tuples (represented as maps)."

  (store-tuple ^Void [this tuple]
    "Stores the tuple represented as map.")

  (query-tuples
    [this criteria tuples-out]
    [this criteria tuples-out lease]
    "Filters tuples by the criteria represented as a map of
     field/value. When a lease channel is passed the result
     channel will keep receiving new tuples until the lease
     emits a value.")
  
  (restarted ^TupleBase [this]))

(defn query-all [tuple-base criteria]
  (let [tuples (chan)]
    (query-tuples tuple-base criteria tuples)
    (async/into [] tuples)))

(defprotocol Database
  (db-create-table [this table columns])
  (db-create-index [this table index-name column-names unique?])
  (db-insert [this table row])
  (db-query [this sql-and-params]))

(extend-protocol Database
  sneer.admin.Database
  (db-create-table [this table columns]
    (.createTable this (name table) columns))
  (db-create-index [this table index-name column-names unique?]
    (.createIndex this (name table) (name index-name) (mapv name column-names) unique?))
  (db-insert [this table row]
    (.insert this (name table) row))
  (db-query [this sql-and-params]
    (.query this (first sql-and-params) (subvec sql-and-params 1))))

(defn- create-tuple-table [db]
  (db-create-table
    db :tuple
    [[:id :integer "PRIMARY KEY"]
     [:type :varchar "NOT NULL"]
     [:payload :blob]
     [:timestamp :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
     [:author :blob "NOT NULL"]
     [:audience :blob]
     [:original_id :integer]
     ;[:device :blob "NOT NULL"]
     ;[:sequence :integer "NOT NULL"]
     ;[:signature :blob "NOT NULL"]
     [:custom :blob]]))

(defn- create-tuple-indices [db]
  (db-create-index db :tuple "idx_tuple_uniqueness" [:author :original_id] true)
  (db-create-index db :tuple "idx_tuple_type" [:type] false))

(defn- create-prik-table [db]
  (db-create-table
    db :keys
    [[:prik :blob]]))

(def builtin-field? #{"type" "payload" "author" "audience"})

(def puk-serializer 
  (let [keys-impl (KeysImpl.)]
    {:serialize #(.toBytes ^sneer.PublicKey %)
     :deserialize #(.createPublicKey keys-impl ^bytes %)}))

(def core-serializer {:serialize serialization/serialize
                      :deserialize serialization/deserialize})

(def serializers {"author" puk-serializer
                  "audience" puk-serializer
                  "payload" core-serializer
                  "custom" core-serializer})

(defn apply-serializer [op row field serializer]
  (let [v (get row field)]
    (if (nil? v)
      row
      (assoc row field ((op serializer) v)))))

(def serialize-entry (partial apply-serializer :serialize))

(def deserialize-entry (partial apply-serializer :deserialize))

(defn apply-serializers [f row]
  (reduce-kv f row serializers))

(defn serialize-entries [row]
  (apply-serializers serialize-entry row))

(defn deserialize-entries [row]
  (apply-serializers deserialize-entry row))

(defn ->custom-field-map [tuple]
  (reduce-kv
    (fn [map k v]
      (if (or (builtin-field? k) (nil? v))
        map
        (assoc map k v)))
    nil
    tuple))

(defn query-for [criteria]
  (let [columns (-> criteria (select-keys builtin-field?) serialize-entries)
        ^String filter (apply str (interpose " AND " (map #(str % " = ?") (keys columns))))
        values (vals columns)]
    (if-some [starting-from (:starting-from criteria)]
      (apply vector (str "SELECT * FROM tuple WHERE id > ? " (when-not (.isEmpty filter) " AND ") filter " ORDER BY id") starting-from values)
      (apply vector (str "SELECT * FROM tuple " (when-not (.isEmpty filter) " WHERE ") filter " ORDER BY id") values))))

(defn query-tuples-from-db [db criteria]
  (let [rs (db-query db (query-for criteria))
        field-names (mapv name (first rs))]
    (->>
      (next rs)
      (map #(deserialize-entries (zipmap field-names %)))
      (map #(merge (get % "custom") (dissoc % "custom"))))))

(defn- insert-tuple [db tuple id]
  (let [custom (->custom-field-map tuple)
        row (select-keys tuple builtin-field?)
        row (assoc row
                   "id" id
                   "original_id" (or (get tuple "id") id)
                   "custom" custom)]
    (db-insert db :tuple (serialize-entries row))))

(defn- try-insert-tuple [db tuple id]
  (try
    (insert-tuple db tuple id)
    true
    (catch java.sql.SQLException e
      (. System/err println e)
      false)))

(defn max-tuple-id [db]
  (let [rs (db-query db ["SELECT MAX(id) FROM tuple"])]
    (or (-> rs second first) 0)))

(defmacro rx-defer [& body]
  `(rx.Observable/defer
     (rx-interop/fn [] ~@body)))

(defn idempotently [creation-fn]
  (try
    (creation-fn)
    (catch Exception e
      (when-not (-> e .getMessage (.contains "already exists"))
        (throw e)))))

(defn setup [db]
  (idempotently #(create-tuple-table db))
  (idempotently #(create-prik-table db))
  (idempotently #(create-tuple-indices db)))

(defn create [db]
  (let [new-tuples (dropping-chan)
        new-tuples-mult (mult new-tuples)
        requests (chan)]

    (setup db)

    (go-trace
      (loop [prev-tuple-id (max-tuple-id db)]
        (when-some [request (<! requests)]
          (match request
            {:store tuple}
            (let [next-tuple-id (inc prev-tuple-id)]
              (recur
               (if (try-insert-tuple db tuple next-tuple-id)
                 (do (>! new-tuples tuple)
                     next-tuple-id)
                 prev-tuple-id)))

            {:query criteria :tuples-out tuples-out}
            (do
              (>! tuples-out (query-tuples-from-db db criteria))
              (recur prev-tuple-id))))))

    (reify TupleBase

      (store-tuple [this tuple]
        (>!! requests {:store tuple}))

      (query-tuples [this criteria tuples-out]
        (let [query-result (chan)]
          (go
            (>! requests {:query criteria :tuples-out query-result})
            (let [tuples (<! query-result)]
              (doseq [tuple tuples]
                (>! tuples-out tuple)))
            (close! tuples-out))))

      (query-tuples [this criteria tuples-out lease]
        (let [new-tuples (dropping-tap new-tuples-mult)
              query-result (chan)
              previous-id (atom 0)]
          (go (<! lease)
              (close! new-tuples))
          (go-loop []
            (>! requests {:query (assoc criteria :starting-from @previous-id) :tuples-out query-result})
            (let [tuples (<! query-result)]
              (when-not (empty? tuples)
                (doseq [tuple tuples]
                  (>! tuples-out tuple))
                (reset! previous-id (-> tuples last (get "id")))))
            ;;TODO: (>! tuples-out :wait-marker)
            (when (<! new-tuples)
              (recur)))))

      (restarted [this]
        (create db)))))
