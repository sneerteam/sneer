(ns sneer.flux.conversation-store
  (:require [rx.lang.clojure.core :as rx])
  (:import [java.util Calendar]))

(gen-class
  :name sneer.flux.ConversationStoreServiceProvider
  :implements [sneer.flux.ConversationStore$Interface]
  :init create
  :state state
  :prefix -interface-
  :constructors {[sneer.admin.SneerAdmin] []})

(defn- summary [party summary timestamp unread]
  (sneer.flux.ConversationStore$Summary. party summary timestamp unread))

(defn -interface-create [admin]
  (let [super-args []
        state {:admin admin}]
    [super-args state]))

(defn -interface-summaries [this]
  (let [state (.state this)
        now (Calendar/getInstance)
        timestamp (.getTimeInMillis now)]
    (rx/return
      [(summary "neide" "oi" timestamp 1)
       (summary "maico" "demorô" (- timestamp (* 120 1000)) 3)
       (summary "alice" "" timestamp 0)])))
