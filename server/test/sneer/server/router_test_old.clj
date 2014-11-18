(ns sneer.server.router-test-old
  (:require
   [clojure.core.async :as async :refer [>! <! >!! <!! alts!! timeout]]
   [clojure.test :refer :all]
   [midje.sweet :refer :all]
   [sneer.server.router-old :as router]
   [sneer.server.io :as io]))

(defn <?!! [c]
  (let [[v _] (alts!! [c (timeout 100)])]
    v))

(defn create-puk [seed]
  seed)

(defn create-router []
  (let [packets-in (async/chan 1)
        packets-out (async/chan 1)]
    (router/start packets-in packets-out)
    {:packets-in packets-in :packets-out packets-out}))

(defn send! [router packet]
  (>!! (:packets-in router) packet))

(defn take?! [router]
  (<?!! (:packets-out router)))

(defn retry! [router]
  #_(>!! (:retry router) ::stimulus))

(def ^:private client-a
  (create-puk "ca"))

(def ^:private client-b
  (create-puk "cb"))

(def ^:private client-c
  (create-puk "cc"))

(defn hash-data [string]
  (str "#" string))

(facts
 "Facts about client/server conversations"

  (fact "to each ping its pong"
        (let [router (create-router)]
          (send! router      {:intent :ping :from client-a})
          (take?! router) => {:intent :pong :to   client-a}))

  #_(fact "sendTo(Puk receiver, byte[] payload) => receiveFrom(Puk sender, byte[] payload)"
         (let [router (create-router)]
           (send! router      {:intent :send    :from client-a :to client-b :data "42"})
           (take?! router) => {:intent :ack     :to   client-a :hash (hash-data "42")}
           (take?! router) => {:intent :send    :to   client-b :data "42"}
           (send! router      {:intent :send    :from client-a :to client-b :data "43"})
           (take?! router) => {:intent :ack     :to   client-a :hash (hash-data "43")}
           (retry! router)
           (take?! router) => {:intent :send    :to   client-b :data "42"}
           )))