(ns sneer.tuple.protocols)

(defprotocol TupleBase
  "A backing store for tuples (represented as maps)."

  (store-tuple
    ^Void [this tuple]
    ^Void [this tuple uniqueness-criteria]
    "Stores the tuple represented as map. When uniqueness-criteria is provided,
     the tuple is stored only if a query by uniqueness-criteria returns an empty set.")

  (query-tuples
    [this criteria tuples-out]
    [this criteria tuples-out lease]
    "Filters tuples by the criteria represented as a map of
     field/value. When a lease channel is passed the result
     channel will keep receiving new tuples until the lease
     emits a value.")

  (set-local-attribute
    ^Void [this attribute value tuple-id])
  (get-local-attribute
    ^Void [this attribute default-value tuple-id response-ch])

  (restarted ^TupleBase [this]))

(defprotocol Database
  (db-create-table [this table columns])
  (db-create-index [this table index-name column-names unique?])
  (db-insert [this table row])
  (db-query [this sql-and-params]))
