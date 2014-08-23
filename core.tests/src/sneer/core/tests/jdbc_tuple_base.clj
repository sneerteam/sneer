(ns sneer.core.tests.jdbc-tuple-base
  (:require [sneer.core :as core]
            [clojure.java.jdbc :as sql]
            [rx.lang.clojure.core :as rx])
  (:import [java.sql DriverManager]))

(defn create-table! [db]
  (let [tuple-ddl
        (sql/create-table-ddl
          :tuple
          [:id :integer "PRIMARY KEY AUTOINCREMENT"]
          [:type :varchar "NOT NULL"]
          [:payload :varchar]
          [:timestamp :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
          [:author :blob "NOT NULL"]
          [:audience :blob]
          ;[:device :blob "NOT NULL"]
          ;[:sequence :integer "NOT NULL"]
          ;[:signature :blob "NOT NULL"]
          [:custom :varchar])]
    (sql/execute! db [tuple-ddl])))

(def builtin-field? #{"type" "payload" "author" "audience"})

(def puk-serializer {:serialize #(.bytes %)
                     :deserialize #(sneer.impl.keys.Keys/createPublicKey %)})

(def serializers {"author" puk-serializer
                  "audience" puk-serializer})

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

(defn query-all [db]
  (sql/query db ["SELECT * FROM tuple"] :as-arrays? true))

(defn dump-tuples [db]
  (->> (query-all db) (map println) doall))

(defn create []
  (let [connection (DriverManager/getConnection "jdbc:sqlite::memory:")
        db {:connection connection}]
    (create-table! db)
    (reify core/TupleBase

      (store-tuple [this tuple]
        (let [custom (->custom-field-map tuple)
              row (select-keys tuple builtin-field?)]
          (sql/insert! db :tuple (serialize-entries (assoc row :custom custom)))
          ;(dump-tuples db)
          ))

      (query-tuples [this criteria keep-alive]
        (let [r (sql/query db ["SELECT * FROM tuple"] :result-set-fn doall :as-arrays? true)
              field-names (mapv name (first r))
              ^java.lang.Iterable tuples (map #(deserialize-entries (zipmap field-names %)) (next r))
              r nil]
          ;(println "query-tuples [" criteria keep-alive "] -> \n\t" (apply str (interpose "\n\t" tuples)))
          (rx.Observable/from tuples))))))
