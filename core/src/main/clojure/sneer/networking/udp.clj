(ns sneer.networking.udp
  (:require [sneer.serialization :refer [serialize deserialize]]
            [sneer.async :refer [go-trace]]
            [clojure.core.async :as async :refer [>! <! >!! <!!]])
  (:import [java.net DatagramPacket DatagramSocket SocketAddress InetAddress InetSocketAddress SocketException]))

(def MTU 1400)  ; Anecdotal suggestions on the web.

(defn new-datagram []
  (new DatagramPacket (byte-array MTU) MTU))

(defn data->value
  "Reads the encoded datagram and returns [socket-address value]"
  [^DatagramPacket datagram]
  (let [address (.getSocketAddress datagram)
        value (deserialize (.getData datagram) (.getLength datagram))]
    #_(println address value)
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

(defn- open-socket [port]
  (if port
    (DatagramSocket. ^int port)
    (DatagramSocket.)))

(defn start-udp-server
  "Opens a UDP socket on port, sending packets taken from packets-out and putting received packets into packets-in.
  Server will stop when packets-out is closed."
  [packets-in packets-out & [port]]

  (when port (println "Opening port" port))

  (let [socket (open-socket port)
        print-err-if-open #(when (is-open socket) (.printStackTrace ^Exception %))]

    (go-trace
      (with-open [^DatagramSocket socket ^DatagramSocket socket]
        (loop []
          (when-let [packet (<! packets-out)]
            (try
               #_(println "<!" packet)
               (send-value socket packet)
               (catch Exception e (print-err-if-open e)))
              (recur))))
      (when port (println "Closing port" port)))

    (async/thread
      (while (is-open socket)
        (try
          (let [packet (receive-value socket)]
            #_(println ">!!" packet)
            (>!! packets-in packet))
          (catch Exception e (print-err-if-open e)))))))
