(ns sneer.tuple.jdbc-database
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as string]
            [sneer.tuple.persistent-tuple-base :as tuple-base])
  (:import [java.sql DriverManager]
           [sneer.admin UniqueConstraintViolated]))

(defn- get-connection [databaseFile]
  (DriverManager/getConnection
   (if databaseFile
     (str "jdbc:sqlite:" (.getAbsolutePath databaseFile))
     "jdbc:sqlite::memory:")))

(defn reify-database [connection]
  (let [db {:connection connection}
        open (atom true)]
    (reify
      tuple-base/Database
      (db-create-table [_ table columns]
        (let [tuple-ddl (apply sql/create-table-ddl table columns)]
          (sql/execute! db [tuple-ddl])))

      (db-create-index [_ table index-name columns-names unique?]
        (sql/execute! db [(str "CREATE" (when unique? " UNIQUE") " INDEX " index-name " ON " (name table) "(" (string/join "," (map name columns-names)) ")" )]))

      (db-insert [_ table row]
        (try
          (sql/insert! db table row)
          (catch java.sql.SQLException e
            ;; [SQLITE_CONSTRAINT] Abort due to constraint violation (UNIQUE constraint failed: tuple.author, tuple.original_id
            (if (.. e getMessage (contains "UNIQUE constraint"))
              (throw (UniqueConstraintViolated. (.getMessage e)))
              (throw e)))))

      (db-query [_ sql-and-params]
        (assert @open)  ;For debugging in 2015/FEB.
        (sql/query db sql-and-params :result-set-fn doall :as-arrays? true))

      java.io.Closeable
      (close [_]
        (reset! open false)
        (.close connection)))))

(defn create-sqlite-db [& [databaseFile]]
  (reify-database(get-connection databaseFile)))

