(ns sneer.networking.udp
  (:require [sneer.serialization :refer [serialize deserialize]]
            [clojure.core.async :as async :refer [>! <! >!! <!!]])
  (:import [java.net DatagramPacket DatagramSocket SocketAddress InetAddress InetSocketAddress SocketException]))


(defn new-datagram []
  (new DatagramPacket (byte-array 1024) 1024))

(defn data->value
  "Reads the encoded datagram and returns [socket-address value]"
  [^DatagramPacket datagram]
  (let [address (.getSocketAddress datagram)
        value (deserialize (.getData datagram) (.getLength datagram))]
    (println address value)
    [address value]))

(defn value->data
  "Returns an encoded datagram of the given value, with the given socket address set."
  [[address value]]
  (doto (^DatagramPacket new-datagram)
    (.setSocketAddress address)
    (.setData (serialize value))))

(defn send-value [socket value]
  (. ^DatagramSocket socket send (value->data value)))

(defn receive-value [socket]
  (let [datagram (new-datagram)]
    (. ^DatagramSocket socket receive datagram)
    (data->value datagram)))

(defn is-open [socket]
  (not (.isClosed ^DatagramSocket socket)))

(defn serve-udp
  "Opens a UDP socket on port, putting received packets into packets-in, sending packets taken from packets-out.
  Server will stop when packets-out is closed."
  [packets-in packets-out & [port]]

  (when port (println "Opening port" port))

  (let [socket (if port (new DatagramSocket ^int port) (new DatagramSocket))
        print-err-if-open #(when (is-open socket) (.printStackTrace ^Exception %))]

    (async/go
      (with-open [socket socket]
        (loop []
          (when-let [packet (<! packets-out)]
            (try
              (println "<!" packet)
              (send-value socket packet)
              (catch Exception e (print-err-if-open e)))
            (recur)))))

    (async/thread
      (while (is-open socket)
        (try
          (let [packet (receive-value socket)]
            (println ">!!" packet)
            (>!! packets-in packet))
          (catch Exception e (print-err-if-open e)))))))

;; Tests

;;(send-ping "localhost" 5555)
;;(send-ping "dynamic.sneer.me" 5555)

(defn send-ping [host port]
  (with-open [socket (new DatagramSocket ^int (+ 10000 port))]
    (let [addr (InetSocketAddress. ^String host ^int port)]
      (send-value socket [addr {:intent :ping}])
      (. socket setSoTimeout 500)
      (let [[_ pong] (receive-value socket)]
        (println pong)))))

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

  (let [socket (new DatagramSocket ^int (+ 10000 echo-port))
        addr (InetSocketAddress. "localhost" ^int echo-port)]
    (send-value socket [addr "Hello"])
    (. socket setSoTimeout 100)
    (let [[_ msg] (receive-value socket)]
      (assert (= msg "Hello")))))

;(run-test)
