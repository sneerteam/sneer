(ns sneer.tuple.jdbc-database
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as string]
            [sneer.tuple.protocols :as tuple-base])
  (:import [java.sql DriverManager SQLException]
           [sneer.admin UniqueConstraintViolated]
           [java.util.concurrent.locks Lock ReentrantReadWriteLock]
           [java.io Closeable]))

(defn- get-connection [databaseFile]
  (DriverManager/getConnection
   (if databaseFile
     (str "jdbc:sqlite:" (.getAbsolutePath databaseFile))
     "jdbc:sqlite::memory:")))

(defmacro with-lock [lock & body]
  `(let [lock# ^Lock ~lock]
     (.lock lock#)
     (try
       ~@body
       (finally
         (.unlock lock#)))))

(defmacro with-read-lock [rw-lock & body]
  `(with-lock (.readLock ~rw-lock) ~@body))

(defmacro with-write-lock [rw-lock & body]
  `(with-lock (.writeLock ~rw-lock) ~@body))


(defn reify-database [connection]
  (let [db {:connection connection}
        rw-lock (ReentrantReadWriteLock.)
        open (atom true)]
    (reify
      tuple-base/Database
      (db-create-table [_ table columns]
        (with-write-lock rw-lock
          (let [tuple-ddl (apply sql/create-table-ddl table columns)]
            (sql/execute! db [tuple-ddl]))))

      (db-create-index [_ table index-name columns-names unique?]
        (with-write-lock rw-lock
          (sql/execute! db [(str "CREATE" (when unique? " UNIQUE") " INDEX " index-name " ON " (name table) "(" (string/join "," (map name columns-names)) ")" )])))

      (db-insert [_ table row]
        (try
          (with-write-lock rw-lock
            (sql/insert! db table row))
          (catch SQLException e
            ;; [SQLITE_CONSTRAINT] Abort due to constraint violation (UNIQUE constraint failed: tuple.author, tuple.original_id
            (if (.. e getMessage (contains "UNIQUE constraint"))
              (throw (UniqueConstraintViolated. (.getMessage e)))
              (throw e)))))

      (db-query [_ sql-and-params]
        (assert @open)  ;For debugging in 2015/FEB.
        (with-read-lock rw-lock
          (sql/query db sql-and-params :result-set-fn doall :as-arrays? true)))

      Closeable
      (close [_]
        (with-write-lock rw-lock
          (reset! open false)
          (.close connection))))))

(defn create-sqlite-db [& [databaseFile]]
  (reify-database(get-connection databaseFile)))

