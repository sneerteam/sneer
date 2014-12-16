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
  (when port (println "Opening port" port))
  (if port
    (DatagramSocket. ^int port)
    (DatagramSocket.)))

(defn- close-socket [socket port]
  (when port (println "closing: " socket))
  (when socket
    (try
      (.close socket)
      (when port (println "closed"))
      (catch Exception e :ignored))))

(defn- produce-socket [port socket-atom status]
  (case status
    :closed
    (do 
      (close-socket @socket-atom port)
      nil)

    :running
    @socket-atom

    :missing
    (do
      (swap! socket-atom
             #(do
                (close-socket % port)
                (open-socket port))))))

(defn- handle-err [socket-atom err port]
  (swap! socket-atom
         (fn [socket]
           (when port (println "error: " err))
           (when (is-open socket)
             (.close socket)
             (.printStackTrace ^Exception err))
           :none)))

(defn start-udp-server
  "Opens a UDP socket on port, sending packets taken from packets-out and putting received packets into packets-in.
  Server will stop when packets-out is closed."
  [packets-in packets-out & [port]]  

  (let [socket-atom (atom nil)
        status (atom :missing)
        with-socket (fn [action]
                      (try
                        (action @socket-atom)
                        (catch Exception e nil)))]

    (async/thread
      (while-let [packet (<!! packets-out)]
        (let [socket @socket-atom]
          (try
            (send-value socket packet)
            (catch Exception e (close-socket socket)))))
      (reset! status :closed))

    (async/thread
      (while-let [socket (produce-socket port socket-atom @status)]
        (try
          (let [packet (receive-value socket)]
            (>!! packets-in packet))
          (catch Exception e (swap! status #(if (= % :closed) :closed :missing))))))))
