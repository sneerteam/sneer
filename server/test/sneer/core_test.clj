(ns sneer.core-test
  (:require
   [clojure.core.async :as async :refer [chan >! <! >!! <!! alts!! timeout]]
   [clojure.test :refer :all]
   [midje.sweet :refer :all]
   [sneer.server.core :as core]
   [sneer.server.io :as io]))

(defn <?!! [c]
  (let [[v _] (alts!! [c (timeout 100)])]
    v))

(defn create-puk [seed]
  seed)

(defn create-server []
  "Creates a packet relay server.

The server can either accept a packet from a client or not in which
case the client should retry later.

After accepting a packet the server guarantees eventual delivery of the packet.

The following operations are supported:

# source to server protocol

peer->server: {:intent :packet :address <destination puk> :sequence <client assigned sequence number> :payload <.> }
server->peer: {:intent :ack :last-sent <sequence> :last-to-send <sequence>}

If both :last-sent and :last-to-send are nil this is the first time the server hears about client
or the server lost its state, the client should send all packets.

peer->server: {:intent :status}
server->peer: same as :ack above

# destination to server protocol

peer->server: {:intent :query-next-packet :puk <puk>}
server->peer: {:intent :packet :packet <packet> :id <server assigned packet id>}
peer->server: {:intent :ack :id <id>}

"
  (atom {}))

(defn route-packet [server-map packet])

(defn route! [server packet])

(defn next-packet-for [puk server])

(facts
 "Facts"
 (let [a (create-puk "a")
       b (create-puk "b")
       server (create-server)
       packet-a {:address a}
       packet-b {:address b}]

   (route! server packet-a)
   (route! server packet-b)

   (fact "server stores packets"
         (next-packet-for a server) => packet-a
         (next-packet-for b server) => packet-b)

   (fact "server forgets sent packets"
         (next-packet-for a server)) => nil))
