(ns sneer.tuple.persistent-tuple-base
  (:import [sneer.commons SystemReport]
           [sneer.admin UniqueConstraintViolated]
           [java.lang AutoCloseable]
           (sneer PublicKey))
  (:require [sneer.commons :refer [now submap?]]
            [sneer.async :refer [dropping-chan go-trace dropping-tap]]
            [clojure.core.async :as async :refer [go-loop <! >! >!! <!! mult tap chan close! go thread]]
            [sneer.rx :refer [filter-by seq->observable]]
            [sneer.rx-macros :refer :all]
            [clojure.core.match :refer [match]]
            [sneer.serialization :as serialization]
            [sneer.tuple.protocols :refer :all]
            [sneer.keys :as keys]))

(def after-id ::after-id)
(def last-by-id ::last-by-id)

(defn query-all [tuple-base criteria]
  (let [tuples (chan)]
    (query-tuples tuple-base criteria tuples)
    (async/into [] tuples)))

(defn- create-tuple-table [db]
  (db-create-table
    db :tuple
    [[:id :integer "PRIMARY KEY"]
     [:type :varchar "NOT NULL"]
     [:payload :blob]
     [:timestamp :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
     [:author :blob "NOT NULL"]
     [:original_id :integer "NOT NULL"]
     [:audience :blob]
     ;[:device :blob "NOT NULL"]
     ;[:sequence :integer "NOT NULL"]
     ;[:signature :blob "NOT NULL"]
     [:custom :blob]]))

(defn- create-attribute-table [db]
  (db-create-table
    db :attribute
    [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
     [:attribute :varchar "NOT NULL"]
     [:tuple_id :integer "NOT NULL"]
     [:value :blob]]))

(defn- create-tuple-indices [db]
  (db-create-index db :tuple "idx_tuple_uniqueness" [:author :original_id] true)
  (db-create-index db :tuple "idx_tuple_type" [:type] false))

(defn- create-attribute-indices [db]
  (db-create-index db :attribute "idx_attribute_tuple" [:tuple_id] false))

(defn- create-prik-table [db]
  (db-create-table
    db :keys
    [[:prik :blob]]))

(def builtin-field? #{"type" "payload" "author" "audience" "timestamp"})

(defn puk-serializer [^PublicKey puk]
  (.toBytes puk))

(defn puk-deserializer [bytes]
  (keys/create-puk bytes))

(def core-serializer serialization/serialize)

(def core-deserializer serialization/deserialize)

(def serializers {"author"   puk-serializer
                  "audience" puk-serializer
                  "payload"  core-serializer
                  "custom"   core-serializer})

(def deserializers {"author"   puk-deserializer
                    "audience" puk-deserializer
                    "payload"  core-deserializer
                    "custom"   core-deserializer})

(defn apply-serializer [row field serializer]
  (let [v (get row field)]
    (if (nil? v)
      row
      (assoc row field (serializer v)))))

(defn serialize-entries [row]
  (reduce-kv apply-serializer row serializers))

(defn deserialize-entries [row]
  (reduce-kv apply-serializer row deserializers))

(defn ->custom-field-map [tuple]
  (reduce-kv
    (fn [map k v]
      (if (or (builtin-field? k) (nil? v))
        map
        (assoc map k v)))
    nil
    tuple))

(defn- query-by-builtin-fields [criteria after-id last-by-id]
  (let [columns (-> criteria (select-keys builtin-field?) serialize-entries)
        clauses (cond-> (map #(str % " = ?") (keys columns))
                  (some? after-id)
                  (conj (str "ID > " after-id)))
        ^String
        filter (apply str (interpose " AND " clauses))
        values (vals columns)
        select (str "SELECT * FROM tuple"
                    (when-not (.isEmpty filter) " WHERE ") filter
                    " ORDER BY id"
                    (when last-by-id " DESC LIMIT 1"))]
    (apply vector select values)))

(defn query-tuples-from-db [db criteria]
  (let [after-id (::after-id criteria)
        criteria (dissoc criteria ::after-id)
        last-by-id (::last-by-id criteria)
        criteria (dissoc criteria ::last-by-id)
        query (query-by-builtin-fields criteria after-id last-by-id)
        rs (db-query db query)
        field-names (mapv name (first rs))
        custom (-> criteria ->custom-field-map)]
    (->>
      (next rs)
      (map #(deserialize-entries (zipmap field-names %)))
      (map #(merge (get % "custom") (dissoc % "custom")))
      (filter #(submap? custom %)))))

(defn- insert-tuple [db tuple id]
  (let [custom (->custom-field-map tuple)
        row (select-keys tuple builtin-field?)
        original-id (or (get tuple "id") id)
        row (assoc row
                   "id" id
                   "original_id" original-id
                   "custom" custom)]
    (db-insert db :tuple (serialize-entries row))
    (assoc tuple "id" id "original_id" original-id)))

(defn- try-insert-tuple [db tuple id]
  (try
    (insert-tuple db tuple id)
    (catch UniqueConstraintViolated _
      (println "try-insert: unique constraint violated")
      nil)
    (catch Exception e
      (println e)
      (SystemReport/updateReport "database/error" e)
      nil)))

(defn- result-empty? [db criteria]
  (let [rs (query-tuples-from-db db criteria)]
    (-> rs empty?)))

(defn max-tuple-id [db]
  (let [rs (db-query db ["SELECT MAX(id) FROM tuple"])]
    (or (-> rs second first) 0)))

(defn idempotently [creation-fn]
  (try
    (creation-fn)
    (catch Exception e
      (when-not (-> e .getMessage (.contains "already exists"))
        (throw e)))))

(defn setup [db]
  (idempotently #(create-tuple-table db))
  (idempotently #(create-attribute-table db))
  (idempotently #(create-attribute-indices db))
  (idempotently #(create-prik-table db))
  (idempotently #(create-tuple-indices db)))

(defn- store! [db uniqueness tuple-id tuple]
  (and (or (nil? uniqueness) (result-empty? db uniqueness))
       (try-insert-tuple db tuple tuple-id)))

(defn- set-attr! [db attribute value tuple-id]
  (try
    (db-insert db :attribute {"attribute" attribute
                              "value"     (serialization/serialize value)
                              "tuple_id"  tuple-id})
    (catch Exception e
      (.printStackTrace e))))

(defn- get-attr! [db attribute default-value tuple-id]
  (println ">>>>> CHANGE TO USE UPDATE <<<<<")
  (let [result-set (db-query db ["SELECT VALUE FROM ATTRIBUTE WHERE TUPLE_ID = ? AND ATTRIBUTE = ? ORDER BY ID DESC LIMIT 1"
                                 tuple-id
                                 attribute])
        value (-> result-set rest first first)]
    (if value
      (serialization/deserialize value)
      default-value)))

(defn- handle-request! [db new-tuples request next-tuple-id]
  (match request
    {:store tuple :tuple-out tuple-out}
    (try
      (when-let [tuple (store! db (:uniqueness request) next-tuple-id tuple)]
        (go-trace (>! new-tuples tuple))
        (>!! tuple-out tuple)
        true)
      (finally
        (close! tuple-out)))

    {:query criteria :tuples-out tuples-out}
    (do (go-trace (>! tuples-out (query-tuples-from-db db criteria)))
        nil)

    {:set-attribute attribute :value value :tuple-id tuple-id}
    (do (set-attr! db attribute value tuple-id)
        nil)

    {:get-attribute attribute :default-value default-value :tuple-id tuple-id :response-ch response-ch}
    (do (go-trace (>! response-ch (get-attr! db attribute default-value tuple-id)))
        nil)))

(defn- server-loop [db requests new-tuples]
  (thread
   (loop [next-tuple-id (-> db max-tuple-id inc)]
     (when-some [request (<!! requests)]
       (let [bump-id (handle-request! db new-tuples request next-tuple-id)]
         (recur (cond-> next-tuple-id bump-id inc)))))))

(defn create [db]
  (setup db)

  (let [new-tuples (dropping-chan)
        new-tuples-mult (mult new-tuples)
        requests (chan 1024)
        running (server-loop db requests new-tuples)

        post-store-tuple-request
        (fn [tuple uniqueness-criteria]
          (let [tuple-out (chan 1)]
            (>!! requests {:store tuple :tuple-out tuple-out :uniqueness uniqueness-criteria})
            tuple-out))]

    (reify TupleBase

      (store-tuple [_ tuple]
        (post-store-tuple-request tuple nil))

      (store-tuple [_ tuple uniqueness-criteria]
        (post-store-tuple-request tuple uniqueness-criteria))

      (query-tuples [_ criteria tuples-out]
        (let [query-result (chan)]
          (go
            (when (>! requests {:query criteria :tuples-out query-result})
              (let [tuples (<! query-result)]
                (doseq [tuple tuples]
                  (>! tuples-out tuple))))
            (close! tuples-out))))

      (query-tuples [_ criteria tuples-out lease]
        (let [new-tuples-in (dropping-tap new-tuples-mult)  ;TODO: Optimization: Transducer: Simply filter the new tuple using the criteria instead of rerunning the DB query. In that case, new-tuples can no longer be a dropping channel and needs a buffer (size 100?) (buffers are LinkedLists so they don't use up space when empty).
              criteria (atom criteria)]
          (go (<! lease)
              (close! new-tuples-in))
          (go-loop []
            (do
              (let [tuples (query-tuples-from-db db @criteria)]
                (when-not (empty? tuples)
                  (doseq [tuple tuples]
                    (>! tuples-out tuple))
                  (swap! criteria assoc ::after-id (-> tuples last (get "id")))))
              (when (<! new-tuples-in)
                (recur))))))

      (query-with-history [_ criteria old-out new-out lease] ;TODO: DRY: Very similar to the query-tuples fn.
        (let [new-tuples-in (dropping-tap new-tuples-mult)  ;TODO: Optimization: Transducer: Simply filter the new tuple using the criteria instead of rerunning the DB query. In that case, new-tuples can no longer be a dropping channel and needs a buffer (size 100?) (buffers are LinkedLists so they don't use up space when empty).
              criteria (atom criteria)]
          (go (<! lease)
              (close! new-tuples-in))
          (go-loop [tuples-out old-out]
            (do
              (let [tuples (query-tuples-from-db db @criteria)]
                (when-not (empty? tuples)
                  (doseq [tuple tuples]
                    (>! tuples-out tuple))
                  (swap! criteria assoc ::after-id (-> tuples last (get "id")))))
              (close! old-out)
              (when (<! new-tuples-in)
                (recur new-out))))))

      (set-local-attribute [_ attribute value tuple-id]
        (>!! requests {:set-attribute attribute
                       :value value
                       :tuple-id tuple-id}))

      (get-local-attribute [_ attribute default-value tuple-id response-ch]
        (>!! requests {:get-attribute attribute
                       :default-value default-value
                       :tuple-id tuple-id
                       :response-ch response-ch}))

      (restarted [this]
        (.close ^AutoCloseable this)
        (create db))

      AutoCloseable
      (close [_]
        (close! requests)
        (close! new-tuples)
        (<!! running)))))



(defn timestamped [proto-tuple]
  (let [max-size 1000]
    (serialization/roundtrip (assoc proto-tuple "timestamp" (now)) max-size)))

(defn store-sub [tuple-base own-puk criteria]
  (let [sub {"type" "sub" "author" own-puk "criteria" criteria}]
    (store-tuple tuple-base (timestamped sub) sub)))