(ns sneer.networking.udp-test
  (:require [midje.sweet :refer :all]
            [sneer.networking.udp :refer [start-udp-server]]
            [sneer.test-util :refer :all]
            [clojure.core.async :as async :refer [chan >!! close!]])
  (:import [java.net InetSocketAddress]))

; (do (require 'midje.repl) (midje.repl/autotest))

(let [echo-port 1024
       loopback (chan)
       packets-out (chan 3)
       packets-in (chan)
       localhost (InetSocketAddress. "localhost" echo-port)
       echo (fn [string]
              (>!! packets-out [localhost (.getBytes string)])
              (let [p (<!!? packets-in)]
                (if (= p :timeout) :timeout (-> p second String.))))]
    
   (start-udp-server loopback loopback echo-port)
   (start-udp-server packets-in packets-out)
    
   (fact "Packets are sent and received"
    (echo "Chance for loopback server to start.")
    (echo "Hello") => "Hello"
    (echo "42") => "42"
    (echo "Goodbye") => "Goodbye")
   
   (close! loopback)
   (close! packets-out))