(ns sneer.interfaces)

(definterface ConvoSummarization
  (slidingSummaries [])
  (processUpToId [id]))
