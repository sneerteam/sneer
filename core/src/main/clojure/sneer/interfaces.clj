(ns sneer.interfaces) ; TODO: Organize namespaces by intent, rather than programming language stereotype.

(definterface ConvoSummarization  ; TODO: Make Contacts and this use the same strategy (interface ou protocol) and make both use interface, protocol or keyword as handle for container.
  (slidingSummaries [])
  (processUpToId [id]))
