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
          [:custom :varchar]
          ;[:device :blob "NOT NULL"]
          ;[:sequence :integer "NOT NULL"]          
          ;[:signature :blob "NOT NULL"]
          [:audience :blob])]
    (sql/execute! db [tuple-ddl])))

(def builtin-field? #{"type" "payload" "author" "audience"})

(defn ->custom-field-map [tuple]
  (->> tuple
    (filter (fn [[key value]] (not (builtin-field? key))))
    (apply concat)
    (apply hash-map)))

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
          (sql/insert! db :tuple (assoc row :custom custom))
          (dump-tuples db)))
      
      (query-tuples [this criteria keep-alive]
        (let [r (sql/query db ["SELECT * FROM tuple"] :result-set-fn doall :as-arrays? true)
              field-names (mapv name (first r))
              ^java.lang.Iterable tuples (map #(apply hash-map (interleave field-names %)) (next r))
              r nil]
          (rx.Observable/from tuples))))))
