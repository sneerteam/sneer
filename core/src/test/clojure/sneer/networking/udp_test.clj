(ns sneer.networking.udp-test
  (:require [midje.sweet :refer :all]
            [sneer.networking.udp :refer [start-udp-server]]
            [sneer.test-util :refer :all]
            [clojure.core.async :as async :refer [chan >!! close!]])
  (:import [java.net InetSocketAddress]))

; (do (require 'midje.repl) (midje.repl/autotest))

(facts
"UDP socket"

 (let [echo-port 1024
       loopback (chan)
       packets-out (chan 3)
       packets-in (chan)
       localhost (InetSocketAddress. "localhost" echo-port)
       echo (fn [string]
              (>!! packets-out [localhost (.getBytes string)])
              (-> (<!!? packets-in) (get 1) String.))]
    
   (start-udp-server loopback loopback echo-port)
   (start-udp-server packets-out packets-in)
    
   (fact "Packets are sent and received"
     (echo "Hello") => "Hello"
     (echo "42") => "42"
     (echo "Goodbye") => "Goodbye")
   
   (close! loopback)
   (close! packets-out)))
