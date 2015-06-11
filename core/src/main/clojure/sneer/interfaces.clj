(ns sneer.interfaces)

(definterface ConvoSummarization
  (slidingSummaries [])
  (getIdByNick [nick])
  (processUpToId [id]))
