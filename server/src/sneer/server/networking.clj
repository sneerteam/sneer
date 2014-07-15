(ns sneer.server.networking
  (:require
   [clojure.edn :as edn]
   [clojure.core.async :as async :refer [chan <! >!! <!!]]
   [sneer.server.core :as core :refer [while-let go!]]
   [sneer.server.io :as io])
  (:import
   [sneer.keys PublicKey]
   [sneer.network.impl NetworkImpl]))

(defn ->value [bytes]
  (edn/read-string (String. bytes "UTF8")))

(defn ->bytes [value]
  (. (pr-str value) getBytes "UTF8"))

(defn send-to-client [server seal message]
  (println (str "OUTGOING/" seal "/" message))
  (>!! (:to-clients @server) [seal (->bytes message)]))

(defn client-network-loop [server seal channel]
  (go!
    (while-let [m (<! channel)]
     (send-to-client server seal m))))

(defn start-pub-sub-client-for [server seal]
  (let [in (async/chan (async/dropping-buffer 1024))
        out (async/chan 1024)
        psc (core/start-client! @server (str seal) in out)]
    (client-network-loop server seal in)
    psc))

(defn pub-sub-client-for [server seal]
  (let [clients (:clients @server)]
    (if-let [existing (get @clients seal)]
      existing
      (let [new-client (start-pub-sub-client-for server seal)]
        (swap! clients assoc seal new-client)
        new-client))))

(defn server-receive [server seal bytes]
  (let [message (->value bytes)
        psc (pub-sub-client-for server seal)]
    (println (str "INCOMING/" seal "/" message))
    (core/post psc message)))

(defn server-client-lost [server seal]
  (println "client lost" seal))

(defn start-network-server [network server-id]
  (let [toClients (chan 1)
        server
        (reify sneer.network.ServerApp
          (publicKey [this]
            (PublicKey/fromHex "DEAD00000000000000000000000000000000000000000000000000000000BEEF"))
          (onError [this exception]
            (println (. exception getMessage)))
          (receive [this public-key bytes]
            (server-receive server-id public-key bytes))

          (nextMessageToSend [this]
            (let [[puk message] (<!! toClients)]
              (basis.Pair/of puk message)))

          (sendCompleted [this public-key bytes])
          (clientLost [this public-key]
            (server-client-lost server-id public-key)))]
    (. network start server)
    toClients))

(defonce network (NetworkImpl.))

(defn start-server! [& [storage]]
  (let [storage (or storage (io/create-temp-dir))
        _ (println "storage is '" storage "'")
        server (atom (core/start-server! storage))
        toClients (start-network-server network server)]
    (swap! server merge {:to-clients toClients :clients (atom {})})))

; *********************************************
; fake client code just to play with the server

;(defonce server (start-server!))

(defn client-public-key [client]
  (:public-key @client))

(defn client-receive [client bytes]
  (println "on-client-receive " client " " (->value bytes)))

(defn connect-client [client-id]
  (NetworkImpl/overrideServerHost "localhost")
  (let [toServer (chan 1)
        client
        (reify sneer.network.ClientApp
          (publicKey [this]
            (client-public-key client-id))
          (receive [this bytes]
            (client-receive client-id bytes))

          (nextMessageToSend [this]
            (<!! toServer))

          (sendCompleted [this bytes])
          (onError [this exception]
            (println (. exception getMessage))))]
    (. network start client)
    toServer))

(defn send-to-server [client bytes]
  (>!! (:to-server @client) bytes))

(def c1 (atom {:public-key (PublicKey/random)}))

(defn edn-key [client]
  (str (:public-key @client)))

(edn-key c1)

(defn connect-test-client! []
  (reset! c1 (merge @c1 {:to-server (connect-client c1)}))
  (send-to-server c1 (->bytes {:tag :sub :id :my-sub :path [(edn-key c1) :foo :bar]}))
  (send-to-server c1 (->bytes {:tag :pub :path [:foo :bar :baz]}))
  (send-to-server c1 (->bytes {:tag :pub :path [:foo :bar :zeng] :value 42})))
;(connect-client!)
