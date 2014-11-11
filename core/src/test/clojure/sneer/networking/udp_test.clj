(ns sneer.networking.udp-test
  (:require [midje.sweet :refer :all]
            [sneer.networking.udp :refer [start-udp-server]]
            [sneer.test-util :refer :all]
            [clojure.core.async :as async :refer [chan]])
  (:import [java.net InetSocketAddress]))

; (do (require 'midje.repl) (midje.repl/autotest))

#_(facts
  "UDP socket"

   (let [echo-port 1024
         echo (chan)
         packets-out (chan 3)
         packets-in (chan)
         localhost (InetSocketAddress. "localhost" echo-port)]
    
     (start-udp-server echo echo echo-port)
     (start-udp-server packets-out packets-in)
    
     (fact "One value is echoed"
       (>!! packets-out [localhost (.getBytes "Hello")])
       (-> (<!!? packets-in) 1 String.) => "Hello")))
