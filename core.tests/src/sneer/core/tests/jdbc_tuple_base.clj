(ns sneer.core.tests.jdbc-tuple-base
  (:require [sneer.core :as core]
            [sneer.rx :refer [filter-by]]
            [sneer.serialization :as serialization]
            [clojure.java.jdbc :as sql]
            [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.interop :as rx-interop])
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
          [:custom :blob])]
    (sql/execute! db [tuple-ddl])))

(def builtin-field? #{"type" "payload" "author" "audience"})

(def puk-serializer {:serialize #(.bytes %)
                     :deserialize #(sneer.impl.keys.Keys/createPublicKey %)})

(def core-serializer {:serialize serialization/serialize
                      :deserialize serialization/deserialize})

(def serializers {"author" puk-serializer
                  "audience" puk-serializer
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

(defn query-all [db]
  (sql/query db ["SELECT * FROM tuple"] :as-arrays? true))

(defn dump-tuples [db]
  (->> (query-all db) (map println) doall))

(defn seq->observable [^java.lang.Iterable iterable]
  (rx.Observable/from iterable))

(defn query-tuples-from-db [db criteria]
  (let [rs (sql/query db ["SELECT * FROM tuple"] :result-set-fn doall :as-arrays? true)
        field-names (mapv name (first rs))]
    (->>
      (next rs)
      (map #(deserialize-entries (zipmap field-names %)))
      (map #(merge (get % "custom") (dissoc % "custom"))))))

(defn insert-tuple [db tuple]
  (let [custom (->custom-field-map tuple)
        row (select-keys tuple builtin-field?)]
    (sql/insert! db :tuple (serialize-entries (assoc row "custom" custom)))))

(defmacro rx-defer [& body]
  `(rx.Observable/defer
     (rx-interop/fn [] ~@body)))

(defn reify-tuple-base [db]
  (let [new-tuples (rx.subjects.PublishSubject/create)]
    
    (reify core/TupleBase

      (store-tuple [this tuple]
        (insert-tuple db tuple)
        (dump-tuples db)
        (rx/on-next new-tuples tuple))

      (query-tuples [this criteria keep-alive]
        (filter-by
          criteria
          (rx-defer
            (let [existing (seq->observable (query-tuples-from-db db criteria))]
              (if keep-alive
                (rx/concat existing (rx/do #(println "[NEW]" %) new-tuples))
                existing))))))))

(defn create []
  (let [connection (DriverManager/getConnection "jdbc:sqlite::memory:")
        db {:connection connection}]
    (create-table! db)
    (reify-tuple-base db)))
