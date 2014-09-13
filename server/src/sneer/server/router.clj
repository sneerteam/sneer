(ns sneer.server.router
  (:require
   [clojure.core.async :as async :refer [>! <!]]
   [clojure.core.match :refer [match]]
   [sneer.server.core :as core :refer [go-while-let]]))

(defn timeout-for [tag]
  (async/timeout
    (case tag
      :offline 30000
      :retry 3000)))

(defn- closed-chan []
  (let [ch (async/chan)]
    (async/close! ch)
    ch))

(def ^:private NEVER (async/chan))

(def ^:private IMMEDIATELY (closed-chan))

(defn- create-queue [packets-in packets-out receiver-heart-beats]
  (let [state (atom {:highest-sequence-delivered -1
                     :packets clojure.lang.PersistentQueue/EMPTY})]
    (letfn
        [(status-to [to]
           {:intent :status-of-queues
            :to to
            :highest-sequence-delivered (highest-sequence-delivered)
            :highest-sequence-to-send (highest-sequence-to-send)
            :full? false})
         (reset-to! [sequence]
           (swap! state merge {:highest-sequence-delivered (dec sequence)
                               :packets clojure.lang.PersistentQueue/EMPTY}))
         (enqueue! [packet]
           (swap! state update-in [:packets] conj (routed packet))
           (status-to (:from packet)))
         (ack! [sequence]
           (swap! state (fn [state]
                          (merge state {:highest-sequence-delivered sequence
                                        :packets (pop (state :packets))}))))
         (highest-sequence-delivered []
           (@state :highest-sequence-delivered))
         (highest-sequence-to-send []
           (let [{:keys [highest-sequence-delivered packets]} @state]
             (+ highest-sequence-delivered (count packets))))
         (valid? [sequence]
           (= sequence (inc (highest-sequence-to-send))))
         (routed [packet]
           (assoc (select-keys packet [:from :to :sequence :payload]) :intent :receive))]

      (let [OFFLINE {:online receiver-heart-beats
                     :keep-alive NEVER
                     :offline NEVER
                     :retry NEVER}]
        (async/go-loop
          [{:keys [online keep-alive offline retry] :as transitions} OFFLINE]

          (async/alt!
            online
              ([_]
                (recur {:online NEVER
                        :keep-alive receiver-heart-beats
                        :offline (timeout-for :offline)
                        :retry IMMEDIATELY}))
            keep-alive
              ([_]
                (recur (assoc transitions :offline (timeout-for :offline))))
            offline
              ([_]
                (recur OFFLINE))
            retry
              ([_]
                (when-let [packet (-> @state :packets peek)]
                  (>! packets-out packet))
                (recur (assoc transitions :retry (timeout-for :retry))))
            packets-in
              ([packet]
                (when packet
                  (match packet
                    {:intent :ack :sequence sequence}
                      (do
                        (when (= sequence (inc (highest-sequence-delivered)))
                          (ack! sequence)
                          (>! packets-out (status-to (:to packet))))
                        (recur (assoc transitions :retry IMMEDIATELY)))
                    :else
                      (do
                        (match packet
                          {:sequence sequence :reset true}
                            (do
                              (reset-to! sequence)
                              (>! packets-out (enqueue! packet)))
                          {:sequence (sequence :guard valid?)}
                            (>! packets-out (enqueue! packet))
                          {:sequence _}
                            (>! packets-out (status-to (:from packet)))
                          :else
                            (>! packets-out (assoc packet :intent :receive)))
                        (recur transitions))))))))
      packets-in)))

(defn packets-from [client mult]
  (async/filter< #(= client (:from %)) (async/tap mult (async/chan))))

(defn start [packets-in packets-out]
  "Store-and-Forward Server Protocol
The purpose of the temporary central server is to store
payloads (byte[]s) being sent from one peer to another in a
limited-size (100 elements?) FIFO queue and forward them as soon as
possible.  Clients must gracefully handle the server losing all its
state at any moment and restarting fresh.

Packets From Client to Server
ping()
Let's the server know the client is
online, when the client has no other useful packet to send. Keeps the
UDP connection open. Server replies with pong().

sendTo(Puk receiver, boolean reset, long sequence, byte[] payload)
If reset is true, the server will discard the entire queue of packets to
be sent to receiver, if any, and use sequence as the next expected
sequence number. The server does not expect any sequence number when it
starts, so the first packet sent from a peer to another must always have
reset true. If sequence is the next expected sequence number, adds
payload to the queue of payloads to be sent from the sender to receiver,
otherwise ignores the payload. Server replies with a statusOfQueues
packet for receiver (see below), even if sequence was wrong.

ack(Puk sender, long sequence)
See receiveFrom(...) below.

queryStatusOfQueues(Puk[] peers)
Server replies with a statusOfQueue packet for peers. See below.

Packets From Server to Client
pong()
Reply to ping() to indicate the server is alive.

receiveFrom(Puk sender, long sequence, byte[] payload)
Client replies with ack(sender, sequence);

statusOfQueues(StatusOfQueue[] statuses)
Indicates de status of the queues of payloads from this client to some
peers. StatusOfQueue is {Puk receiver, long highest-sequence-delivered,
long highest-sequence-to-send, boolean isFull}. The client can consider
all payloads up to highest-sequence-delivered as received by the
receiver. The client can consider all payloads up to
highest-sequence-to-send as received by the server and temporarily (the
server might crash and restart fresh) queued to be sent to
receiver. This packet is sent as a reply to queryStatusOfQueues(...), as
a reply to sendTo(...) and when the receiver
"
  (let [queues (atom {})
        mult-packets-in (async/mult packets-in)
        packets-in (async/tap mult-packets-in (async/chan))]
    (letfn
        [(ensure-queue [to queue]
           (if queue
             queue
             (create-queue (async/chan (async/dropping-buffer 1))
                           packets-out
                           (packets-from to mult-packets-in))))
         (produce-queue [from to]
           (let [path [from to]]
             (get-in (swap! queues update-in path (partial ensure-queue to)) path)))]

      (go-while-let
       [packet (<! packets-in)]
       (println "router/<!" packet)
       (match packet
         {:intent :ping :from from} (>! packets-out {:intent :pong :to from})
         {:intent :send :from from :to to} (>! (produce-queue from to) packet)
         {:intent :ack  :from from :to to} (>! (produce-queue to from) packet))))))
