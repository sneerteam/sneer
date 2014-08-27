(ns sneer.core.tests.jdbc-tuple-base
  (:require [clojure.java.jdbc :as sql]
            [sneer.persistent-tuple-base :as tuple-base]
            [sneer.core :as core])
  (:import [java.sql DriverManager]))

(defn create-sqlite-db [databaseFile]
  (let [connection (DriverManager/getConnection (if databaseFile (str "jdbc:sqlite:" (.getAbsolutePath databaseFile)) "jdbc:sqlite::memory:"))
        db {:connection connection}]
  (reify 
    tuple-base/Database
    (db-create-table [this table columns]
      (let [tuple-ddl (apply sql/create-table-ddl table columns)]
         (sql/execute! db [tuple-ddl])))
    
    (db-insert [this table row]
      (sql/insert! db table row))
    
    (db-query [this sql-and-params]
      (sql/query db sql-and-params :result-set-fn doall :as-arrays? true))

    core/Disposable
    (dispose [this]
      (.close connection)))))

(defn create [& [databaseFile]]
  (let [db (create-sqlite-db databaseFile)]
    (tuple-base/create-tuple-table db)
    (tuple-base/create db)))

