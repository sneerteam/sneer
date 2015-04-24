(ns sneer.server.network)

(defprotocol Network
  (connect
    [network puk tuple-base]
    "Connects tuple-base to the network."))
