(ns sneer.impl.CoreLoader
  (:gen-class
    :implements [sneer.commons.Container$ComponentLoader]))

(defn -load [this component-interface container]
;  (println component-interface "::" LeaseHolder)
  (condp = component-interface

    ; Irregular:
    sneer.tuple.protocols.Database
    (sneer.tuple.jdbc-database/reify-Database container)

    ; Regular:
    sneer.admin.SneerAdmin
    (sneer.admin/reify-SneerAdmin container)

    sneer.async.LeaseHolder
    (sneer.async/reify-LeaseHolder container)

    sneer.commons.ActionBus
    (sneer.commons/reify-ActionBus container)

    sneer.conversations.ConversationList
    (sneer.conversations/reify-ConversationList container)))