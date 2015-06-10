(ns sneer.impl.CoreLoader
  (:gen-class
    :implements [sneer.commons.Container$ComponentLoader]))

(defn -load [this component-interface container]
  (condp = component-interface

    sneer.admin.SneerAdmin
    (sneer.admin/reify-SneerAdmin container)

    sneer.flux.LeaseHolder
    (sneer.flux/reify-LeaseHolder container)

    sneer.commons.ActionBus
    (sneer.flux/reify-ActionBus container)

    sneer.convos.Convos
    (sneer.convos/reify-Convos container)

    sneer.convo_summarization.ConvoSummarization
    (sneer.convo-summarization/reify-ConvoSummarization container)))