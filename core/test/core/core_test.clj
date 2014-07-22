(ns core.core-test
  (:require [clojure.test :refer :all]
            [core.core :refer :all]
            [clojure.java.jdbc :as sql]))

(def db {:classname "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname "db/database.db"})

(defn tuple-ddl []
  (sql/create-table-ddl
   :tuple
   [:id :integer "PRIMARY KEY AUTOINCREMENT"]
   [:type :varchar "NOT NULL"]
   [:value :varchar]
   [:timestamp :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
   [:author :blob "NOT NULL"]
   [:device :blob "NOT NULL"]
   [:sequence :integer "NOT NULL"]
   [:audience :blob "NOT NULL"]
   [:signature :blob "NOT NULL"]))

(defn create-table! []
  (sql/execute! db [(tuple-ddl)]))

(defn insert-message! []
  (let [m {:type :chat-message
           :author "fffff"
           :value "opa"
           :device "device"
           :sequence 1
           :audience "audience"
           :signature "signature"}]
    (sql/insert! db :tuple m)))

(defn reset-db! []
  (let [file (java.io.File. (:subname db))]
    (. file delete)
    (.. file getParentFile mkdirs)))

(deftest tuples
  (testing "can insert and retrieve tuples"
    (reset-db!)
    (create-table!)
    (insert-message!)
    (let [r (sql/query db ["SELECT id FROM tuple WHERE type = ?" :chat-message])]
      (is (= [{:id 1}] r)))))
