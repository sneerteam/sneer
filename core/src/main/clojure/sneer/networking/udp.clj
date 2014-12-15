(ns sneer.networking.udp
  (:require [sneer.serialization :refer [serialize deserialize]]
            [sneer.async :refer [go-trace]]
            [clojure.core.async :as async :refer [>! <! >!! <!!]]
            [sneer.commons :refer :all])
  (:import [java.net DatagramPacket DatagramSocket SocketAddress InetAddress InetSocketAddress SocketException]
           [sneer.commons SystemReport]))

(def MTU 1400)  ; Anecdotal suggestions on the web.

(defn- new-datagram []
  (DatagramPacket. (byte-array MTU) MTU))

(defn- ->value
  "Reads the encoded datagram and returns [socket-address value]"
  [^DatagramPacket datagram]
  (let [address (.getSocketAddress datagram)
        value (deserialize (.getData datagram) (.getLength datagram))]
    #_(println address value)
    [address value]))

(defn- ->datagram
  "Returns an encoded datagram of the given value, with the given socket address set."
  [[address value]]
  (doto (^DatagramPacket new-datagram)
    (.setSocketAddress address)
    (.setData (serialize value))))

(defn- send-value [socket value]
  (.send ^DatagramSocket socket (->datagram value)))

(defn- receive-value [socket]
  (let [datagram (new-datagram)]
   (.receive ^DatagramSocket socket datagram)
   (->value datagram)))

(defn- is-open [socket]
  (and (not (#{:none :closed} socket))
       (not (.isClosed ^DatagramSocket socket))))

(defn- open-socket [port]
  (if port
    (DatagramSocket. ^int port)
    (DatagramSocket.)))

(defn- close-socket [socket]
  (when (is-open socket)
    (try
      (.close socket)
      (catch Exception e :ignored)))
  :closed)

(defn- produce-socket [port socket-atom]
  (swap! socket-atom #(if (= % :none) (open-socket port) %)))

(defn- handle-err [socket-atom err]
  (swap! socket-atom
         (fn [socket]
           (when (is-open socket)
             (.close socket)
             (.printStackTrace ^Exception err))
           :none)))

(defn start-udp-server
  "Opens a UDP socket on port, sending packets taken from packets-out and putting received packets into packets-in.
  Server will stop when packets-out is closed."
  [packets-in packets-out & [port]]

  (when port (println "Opening port" port))

  (let [^DatagramSocket socket (atom :none)]

    (async/thread
      (loop []
        (if-let [packet (<!! packets-out)]
          (do
            (try
              #_(println "<!!" packet)
              (send-value socket packet)
              (catch Exception e (handle-err socket e)))
            (recur))
          (do
            (when port (println "Closing port" port))
            (swap! socket close-socket)))))

    (async/thread
      (while-let [s (produce-socket port socket)]
        (try
          (let [packet (receive-value s)]
            #_(println ">!!" packet)
            (>!! packets-in packet))
          (catch Exception e (handle-err socket e)))))))
