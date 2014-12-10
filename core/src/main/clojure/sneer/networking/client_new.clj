(ns sneer.networking.client-new
  (:import [sneer.commons SystemReport]))

(defn connect-to-follower [client follower-puk tuples-out]
  "client is what is returned by start-client, below.
   Throws IllegalState if there already is a connection to that follower."
  )

(defn start-client [own-puk packets-in packets-out tuples-received]
  )