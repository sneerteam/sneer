(ns sneer.server.udp
  (:require [clojure.edn :as edn])
  (:import [java.net DatagramPacket DatagramSocket SocketAddress InetAddress InetSocketAddress SocketException]))


(defn read-bytes [bytes length]
  (edn/read-string (String. bytes 0 length "UTF8")))


(defn ->bytes [value]
  (. (pr-str value) getBytes "UTF8"))


(defn new-datagram []
  (new DatagramPacket (byte-array 1024) 1024))


(defn data->value
  "Reads the edn-encoded datagram and returns [socket-address value]"
  [datagram]
  (let [address (. datagram getSocketAddress)
        value (read-bytes (. datagram getData) (. datagram getLength))]
    (println address value)
    [address value]))


(defn value->data
  "Returns an EDN-encoded datagram of the given value, with the given socket address set."
  [[address value]]
  (doto (new-datagram)
    (.setSocketAddress address)
    (.setData (->bytes value))))


(defn send-value [socket value]
  (. socket send (value->data value)))

(defn receive-value [socket]
  (let [datagram (new-datagram)]
    (. socket receive datagram)
    (data->value datagram)))


(defn try-to-serve-request [service socket]
  (let [request (receive-value socket)
        replies (service request)]
    (doseq [reply replies]
      (send-value socket reply))))

(defn serve-request [service socket]
  (try
    (try-to-serve-request service socket)
    (catch Exception e (. e printStackTrace))))

(defn serve-udp
  "Opens a UDP socket on port and returns two channels as a vector: [datagrams-in datagrams-out]."
  [service port]
  (println "Opening port " port)
  (let [socket (new DatagramSocket port)]
    (while true
      (serve-request service socket))))



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
