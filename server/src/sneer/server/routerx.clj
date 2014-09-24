(ns sneer.server.routerx
  (:require
   [clojure.core.async :as async :refer [>! <! go-loop alt!]]
   [clojure.core.match :refer [match]]))

(defn timeout-for [transition]
  (async/timeout
    (case transition
      :offline 30000
      :retry 3000)))

(defn reset-to [sequence state]
  (merge state {:highest-sequence-delivered (dec sequence)
                :packets clojure.lang.PersistentQueue/EMPTY}))

(def EMPTY (reset-to 0 {}))

(defn next-packet [state]
  (-> state :packets peek))

(defn routed [packet]
  (assoc (select-keys packet [:from :to :sequence :payload])
         :intent :receive))

(defn highest-sequence-to-send [state]
  (if-some [packet (-> state :packets last)]
    (:sequence packet)
    (:highest-sequence-delivered state)))

(defn enqueue [packet state]
  (if (= (:sequence packet) (inc (highest-sequence-to-send state)))
    (update-in state [:packets] conj (routed packet))
    state))

(defn ack [sequence state]
  (let [packet-to-ack (next-packet state)]
    (if (and (some? packet-to-ack) (= sequence (:sequence packet-to-ack)))
      (merge state {:highest-sequence-delivered sequence :packets (pop (state :packets))})
      state)))

(defn status-to [to follower state]
  {:intent :status-of-queues
   :to to
   :follower follower
   :highest-sequence-delivered (:highest-sequence-delivered state)
   :highest-sequence-to-send (highest-sequence-to-send state)
   :full? false})

(defn handle-packet [packet state]
  (match packet
    {:intent :ack :sequence sequence :to to :from follower}
    (let [state (ack sequence state)
          reply (status-to to follower state)]
      [reply state])
    {:sequence sequence :reset true :from from :to follower}
    (let [state (->> state (reset-to sequence) (enqueue packet))
          reply (status-to from follower state)]
      [reply state])
    {:sequence sequence :from from :to follower}
    (let [state (->> state (enqueue packet))
          reply (status-to from follower state)]
      [reply state])
    :else
      [(assoc packet :intent :receive) state]))

(defn- closed-chan []
  (let [ch (async/chan)]
    (async/close! ch)
    ch))

(def ^:private IMMEDIATELY (closed-chan))

(defn- online-loop [state packets-in packets-out keep-alive]
  (go-loop [state state
            offline (timeout-for :offline)
            retry IMMEDIATELY]
    (alt!
      offline
      ([_] state)

      keep-alive
      ([_] (recur state (timeout-for :offline) retry))

      retry
      ([_]
         (when-some [packet (next-packet state)]
           (>! packets-out packet))
         (recur state offline (timeout-for :retry)))

      packets-in
      ([packet]
         (if (some? packet)
           (let [[reply state] (handle-packet packet state)
                 retry (if (= (packet :intent) :ack) IMMEDIATELY retry)]
             (>! packets-out reply)
             (recur state offline retry))
           state)))))

(defn- create-queue [packets-in packets-out receiver-heartbeats]
  (go-loop [state EMPTY]
    (alt!
      packets-in
      ([packet]
         (when (some? packet)
           (let [[reply state] (handle-packet packet state)]
             (>! packets-out reply)
             (recur state))))

      receiver-heartbeats
      ([heartbeat]
         (when (some? heartbeat)
           (recur
            (<! (online-loop state packets-in packets-out receiver-heartbeats))))))))

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
  (let [mult-packets-in (async/mult packets-in)
        packets-in (async/tap mult-packets-in (async/chan))
        heartbeats-of (fn [client]
                        (async/filter< #(= client (:from %))
                                       (async/tap mult-packets-in (async/chan))
                                       (async/dropping-buffer 1)))
        ensure-queue (fn [queue to]
                       (if queue
                         queue
                         (let [q-packets-in (async/chan (async/sliding-buffer 1))]
                           (create-queue q-packets-in
                                         packets-out
                                         (heartbeats-of to))
                           q-packets-in)))
        produce-queue (fn [from to queues]
                        (let [path [from to]
                              queues (update-in queues path ensure-queue to)]
                          [(get-in queues path) queues]))]
    (go-loop [queues {}]
      (when-some [packet (<! packets-in)]
        (recur
          (match packet
            {:intent :ping :from from}
            (do
              (>! packets-out {:intent :pong :to from})
              queues)
            {:intent :send :from from :to to}
            (let [[q queues] (produce-queue from to queues)]
              (>! q packet)
              queues)
            {:intent :ack :from from :to to}
            (let [[q queues] (produce-queue to from queues)]
              (>! q packet)
              queues)
            :else
            queues))))))
