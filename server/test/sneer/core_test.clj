(ns sneer.core-test
  (:require
   [clojure.core.async :as async :refer [>! <! >!! <!! alts!! timeout]]
   [clojure.test :refer :all]
   [midje.sweet :refer :all]
   [sneer.server.router :as router]
   [sneer.server.io :as io]))

(defn <?!! [c]
  (let [[v _] (alts!! [c (timeout 100)])]
    v))

(defn create-puk [seed]
  seed)

(defn create-router []
  (let [packets-in (async/chan 1)
        packets-out (async/chan)]
    (router/start packets-in packets-out)
    {:packets-in packets-in :packets-out packets-out}))

(defn send! [router packet]
  (>!! (:packets-in router) packet))

(defn take?! [router]
  (<?!! (:packets-out router)))

(facts
 "Facts about client/server conversations"

 (fact "to each ping its pong"
       (let [client (create-puk "c")
             router (create-router)]
         (send! router {:intent :ping :from client})
         (take?! router) => {:intent :pong :to client}))

 (fact "sendTo(Puk receiver, byte[] payload) => receiveFrom(Puk sender, byte[] payload)"
       (let [client-a (create-puk "ca")
             client-b (create-puk "cb")
             router (create-router)]
         (send! router {:intent :send :to client-b :from client-a :payload "42"})
         (take?! router) => {:intent :receive :from client-a :to client-b :payload "42"})))
