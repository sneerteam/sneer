(ns sneer.server.io)

(defn create-temp-dir []
  (doto (java.io.File/createTempFile "sneer-" "-storage")
    (.delete)
    (.mkdir)))
