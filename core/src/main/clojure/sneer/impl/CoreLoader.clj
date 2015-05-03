(ns sneer.impl.CoreLoader
  (:gen-class
    :implements [sneer.commons.Container$ComponentLoader])
  (:require [rx.lang.clojure.core :as rx]
            [sneer.flux.conversation-store :as convo-list]))

(defn -load [this component-interface]
  (condp = component-interface
    sneer.commons.ActionBus              (reify sneer.commons.ActionBus)
    sneer.conversations.ConversationList (reify sneer.conversations.ConversationList
                                           (summaries [this]
                                             (convo-list/do-summaries this)))))