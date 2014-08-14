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

(defn route-packet [server-map packet])

(defn route! [server packet])

(defn next-packet-for [puk server])

(facts
 "Facts about client/server conversations"

 (fact "for each ping its pong"
       (let [client (create-puk "c")
             packets-in (async/chan 1)
             packets-out (async/chan)
             router (router/create-router packets-in packets-out)]
         (>!! packets-in {:intent :ping :from client})
         (<?!! packets-out) => {:intent :pong :to client}))

 (fact "sendTo(Puk receiver, byte[] payload) => receiveFrom(Puk sender, byte[] payload)"
       (let [client-a (create-puk "ca")
             client-b (create-puk "cb")
             packets-in (async/chan 1)
             packets-out (async/chan)
             router (router/create-router packets-in packets-out)]
         (>!! packets-in {:intent :send :to client-b :from client-a :payload "42"})
         (<?!! packets-out) => {:intent :receive :from client-a :to client-b :payload "42"})))
