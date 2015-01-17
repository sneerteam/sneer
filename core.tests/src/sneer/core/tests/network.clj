(ns sneer.core.tests.network)

(defprotocol Network
  (connect
    [network puk tuple-base]
    "Connects tuple-base to the network."))
