(ns sneer.core.tests.jdbc-tuple-base
  (:require [clojure.java.jdbc :as sql]
            [sneer.persistent-tuple-base :as tuple-base])
  (:import [java.sql DriverManager]))

(defn create-sqlite-db [db]
  (reify tuple-base/Database
    (db-create-table [this table columns]
      (let [tuple-ddl (apply sql/create-table-ddl table columns)]
        (sql/execute! db [tuple-ddl])))
    
    (db-insert [this table row]
      (sql/insert! db table row))
    
    (db-query [this sql-and-params]
      (sql/query db sql-and-params :result-set-fn doall :as-arrays? true))))

(defn create []
  (let [connection (DriverManager/getConnection "jdbc:sqlite::memory:")
        db (create-sqlite-db {:connection connection})]
    (tuple-base/create-tuple-table db)
    (tuple-base/create db)))
