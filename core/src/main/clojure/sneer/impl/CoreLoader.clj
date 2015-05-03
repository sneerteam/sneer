(ns sneer.impl.CoreLoader
  (:gen-class
    :implements [sneer.commons.Container$ComponentLoader]))

(defn -load [this component-interface]
  (condp = component-interface
    sneer.commons.ActionBus
    (sneer.commons/reify-ActionBus)

    sneer.conversations.ConversationList
    (sneer.conversations/reify-ConversationList)))