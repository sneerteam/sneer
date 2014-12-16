(ns sneer.tuple.persistent-tuple-base-test
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as string]
            [sneer.tuple.persistent-tuple-base :as tuple-base :refer [query-tuples store-tuple]]
            [sneer.core :as core]            
            [sneer.test-util :refer [<!!?]]
            [midje.sweet :refer :all]
            [clojure.core.async :as async])
  (:import [java.sql DriverManager]
           [sneer.crypto.impl KeysImpl]))

;  (do (require 'midje.repl) (midje.repl/autotest))

(defn create-sqlite-db [databaseFile]
  (let [connection (DriverManager/getConnection (if databaseFile (str "jdbc:sqlite:" (.getAbsolutePath databaseFile)) "jdbc:sqlite::memory:"))
        db {:connection connection}]
    (reify
      tuple-base/Database
      (db-create-table [this table columns]
        (let [tuple-ddl (apply sql/create-table-ddl table columns)]
          (sql/execute! db [tuple-ddl])))

      (db-create-index [this table index-name columns-names]
        (sql/execute! db [(str "CREATE INDEX " index-name " ON " (name table) "(" (string/join "," (map name columns-names)) ")" )]))

      (db-insert [this table row]
        (sql/insert! db table row))

      (db-query [this sql-and-params]
        (sql/query db sql-and-params :result-set-fn doall :as-arrays? true))

      core/Disposable
      (dispose [this]
        (.close connection)))))

(defn create [& [databaseFile]]
  (let [db (create-sqlite-db databaseFile)]
    (tuple-base/create db)))

(defn- create-puk [^bytes rep] (.createPublicKey (KeysImpl.) rep))

(facts "About query-tuples"
  (let [subject (create)
        result (async/chan)
        lease (async/chan)
        t1 {"type" "sub" "payload" "42" "author" (create-puk (.getBytes "neide"))}
        select-t1-keys #(select-keys % (keys t1))
        query (query-tuples subject {"type" "sub"} true result lease)]

    (fact "When keep-alive is true it sends new tuples"
      (store-tuple subject t1)
      (-> (<!!? result) select-t1-keys) => t1)

    (fact "When lease channel is closed query-tuples is terminated"
      (async/close! lease)
      (<!!? query) => nil)))
