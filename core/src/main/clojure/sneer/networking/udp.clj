(ns sneer.networking.udp
  (:require [sneer.serialization :refer [serialize deserialize]]
            [clojure.core.async :as async :refer [>! <! >!! <!!]]
            [sneer.commons :refer :all])
  (:import [java.net DatagramPacket DatagramSocket]
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
  (and socket (not (.isClosed ^DatagramSocket socket))))

(defn- open-socket [port]
  (when port (println "Opening port" port))
  (if port
    (DatagramSocket. ^int port)
    (DatagramSocket.)))

(defn- close-socket [port ^DatagramSocket socket]
  (when (is-open socket)
    (when port (println "Closing port" port))
    (try
      (.close socket)
      (catch Exception _ :ignored))))

(defn on-open-error [^Throwable exception]
  (println "Error opening socket:")
  (.printStackTrace exception)
  (SystemReport/updateReport "network/open" exception)
  (Thread/sleep 3000))

(defn- produce-socket [port socket-atom closed?]
  (loop []
    (let [current @socket-atom]
      (if @closed?
        (do
          (close-socket port current)
          nil)
        (if (is-open current)
          current
          (do
            (try
              (reset! socket-atom (open-socket port))
              (catch Exception e (on-open-error e)))
            (recur))))))) ; Make sure closed? has not been set to true.
  
(defn- close-on-err [port socket socket-operation]
  (try
    (socket-operation socket)
    (catch Exception e
      (when (is-open socket)
        (println (.getMessage e))
        (close-socket port socket)))))

(defn start-udp-server
  "Opens a UDP socket on port, sending packets taken from packets-out and putting received packets into packets-in.
  Server will stop when packets-out is closed."
  [packets-in packets-out & [port]]  

  (let [socket-atom (atom nil)
        closed? (atom false)]

    (async/thread
      (while-let [packet (<!! packets-out)]
        (when-let [socket @socket-atom]
          (close-on-err port socket #(send-value % packet))))
      (reset! closed? true)
      (close-socket port @socket-atom))

    (async/thread
      (while-let [socket (produce-socket port socket-atom closed?)]
        (close-on-err port socket #(>!! packets-in (receive-value %)))))))
