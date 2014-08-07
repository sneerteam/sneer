(ns sneer.server.udp
  (:require [sneer.serialization :refer [serialize deserialize]]
            [clojure.core.async :as async :refer [>! <! >!! <!!]])
  (:import [java.net DatagramPacket DatagramSocket SocketAddress InetAddress InetSocketAddress SocketException]))


(defn new-datagram []
  (new DatagramPacket (byte-array 1024) 1024))


(defn data->value
  "Reads the encoded datagram and returns [socket-address value]"
  [datagram]
  (let [address (. datagram getSocketAddress)
        value (deserialize (. datagram getData) (. datagram getLength))]
    (println address value)
    [address value]))


(defn value->data
  "Returns an encoded datagram of the given value, with the given socket address set."
  [[address value]]
  (doto (new-datagram)
    (.setSocketAddress address)
    (.setData (serialize value))))


(defn send-value [socket value]
  (. socket send (value->data value)))


(defn receive-value [socket]
  (let [datagram (new-datagram)]
    (. socket receive datagram)
    (data->value datagram)))


(defn serve-udp
  "Opens a UDP socket on port, putting received packets into packets-in, sending packets taken from packets-out.
  Server will stop when packets-out is closed."
  [port packets-in packets-out]
  (println "Opening port " port)
  (let [socket (new DatagramSocket port)]

    (async/thread
      (while true
        (try
          (let [packet (receive-value socket)]
            (>!! packets-in packet))
          (catch Exception e (. e printStackTrace)))))

    (async/go
      (with-open [socket socket]
        (loop []
          (when-let [packet (<! packets-out)]
            (try
              (send-value socket packet)
              (catch Exception e (. e printStackTrace)))
            (recur)))))))

;; Tests

(defn start-echo-server
  "Starts an echo UDP server that echoes whatever UDP packet it receives."
  [port]
  (future (serve-udp #(do [%]) port)))

(def echo-port 4463)

(defn run-test []
  (let [address (InetSocketAddress. "localhost" 1234)]
    (assert (= [address "banana"]
               (-> [address "banana"] value->data data->value))))

  (start-echo-server echo-port)

  (let [socket (new DatagramSocket (+ 10000 echo-port))
        addr (InetSocketAddress. "localhost" echo-port)]
    (send-value socket [addr "Hello"])
    (. socket setSoTimeout 100)
    (let [[_ msg] (receive-value socket)]
      (assert (= msg "Hello")))))

;(run-test)
