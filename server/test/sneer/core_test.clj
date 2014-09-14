(ns sneer.core-test
  (:require
   [clojure.core.async :as async :refer [>! <! >!! <!! alts!! timeout]]
   [clojure.test :refer :all]
   [midje.sweet :refer :all]
   [sneer.server.routerx :as router]
   [sneer.server.io :as io]))

(defn <?!! [c]
  (let [[v _] (alts!! [c (timeout 100)])]
    v))

(defn create-puk [seed]
  seed)

(defn create-router []
  (let [packets-in (async/chan 1)
        packets-out (async/chan 1)]
    (router/start packets-in packets-out)
    {:packets-in packets-in :packets-out packets-out}))

(defn send! [router packet]
  (>!! (:packets-in router) packet))

(defn take?! [router]
  (<?!! (:packets-out router)))

(def ^:private client-a
  (create-puk "ca"))

(def ^:private client-b
  (create-puk "cb"))

(defn dropping-chan []
  (async/chan (async/dropping-buffer 1)))

(defn packets-to [client mult]
  (async/filter< #(= client (:to %)) (async/tap mult (async/chan 1))))

(facts
 "Facts about client/server conversations"

  (fact "to each ping its pong"
        (let [router (create-router)]
          (send! router {:intent :ping :from client-a})
          (take?! router) => {:intent :pong :to client-a}))

  (fact "sendTo(Puk receiver, byte[] payload) => receiveFrom(Puk sender, byte[] payload)"
        (let [router (create-router)]
          (send! router {:intent :send :to client-b :from client-a :payload "42"})
          (take?! router) => {:intent :receive :from client-a :to client-b :payload "42"})))

(facts
  "sendTo(Puk receiver, boolean reset, long sequence, byte[] payload)

 If sequence is the next expected sequence number, adds
payload to the queue of payloads to be sent from the sender to receiver,
otherwise ignores the payload. Server replies with a statusOfQueues
packet for receiver (see below), even if sequence was wrong."

  (fact "The server expects sequence number 0 when it starts and rejects incorrect sequences."
        (let [router (create-router)]
          (send! router {:intent :send :to client-b :from client-a :payload "foo" :sequence 42})
          (take?! router) => {:intent :status-of-queues
                              :to client-a
                              :highest-sequence-to-send -1
                              :highest-sequence-delivered -1
                              :full? false}))

  (fact "If reset is true, the server will discard the entire queue of packets to
be sent to receiver, if any, and use sequence as the next expected
sequence number."
        (let [router (create-router)]
          (send! router {:intent :send :from client-a :to client-b :payload "foo" :reset true :sequence 42})
          (take?! router) => {:intent :status-of-queues
                              :to client-a
                              :highest-sequence-delivered 41
                              :highest-sequence-to-send 42
                              :full? false}))

  (fact "Acknowledge from receiver causes an updated status-of-queues to be sent to sender."
       (let [router (create-router)
             route! (partial send! router)
             packets-out (async/mult (:packets-out router))
             to-a (packets-to client-a packets-out)]
         (route! {:intent :send :from client-a :to client-b :payload "foo" :sequence 0})
         (<?!! to-a) => {:intent :status-of-queues
                         :to client-a
                         :highest-sequence-delivered -1
                         :highest-sequence-to-send 0
                         :full? false}
         (route! {:intent :ack :from client-b :to client-a :sequence 0})
         (<?!! to-a) => {:intent :status-of-queues
                         :to client-a
                         :highest-sequence-delivered 0
                         :highest-sequence-to-send 0
                         :full? false})))

(facts
  "about receiveFrom(Puk sender, long sequence, byte[] payload)"

  (fact
    "keeps retrying until receiver sends ack"

    (let [clocks {:offline (dropping-chan)
                  :retry (dropping-chan)}]

      (with-redefs [router/timeout-for (partial clocks)]

        (let [router (create-router)
              route! (partial send! router)
              packets-out (async/mult (:packets-out router))
              to-a (packets-to client-a packets-out)
              to-b (packets-to client-b packets-out)]

          (route! {:intent :send :from client-a :to client-b :payload "foo" :sequence 0})
          (<?!! to-a) => {:intent :status-of-queues
                          :to client-a
                          :highest-sequence-delivered -1
                          :highest-sequence-to-send 0
                          :full? false}
          (route! {:intent :send :from client-a :to client-b :payload "bar" :sequence 1})
          (<?!! to-a) => {:intent :status-of-queues
                          :to client-a
                          :highest-sequence-delivered -1
                          :highest-sequence-to-send 1
                          :full? false}
          (route! {:intent :ping :from client-b})
          (<?!! to-b) => {:intent :pong :to client-b}
          (<?!! to-b) => {:intent :receive :from client-a :to client-b :sequence 0 :payload "foo"}
          (>!! (clocks :retry) :tick)
          (<?!! to-b) => {:intent :receive :from client-a :to client-b :sequence 0 :payload "foo"}
          (route! {:intent :ack :from client-b :to client-a :sequence 0})
          (<?!! to-a) => {:intent :status-of-queues
                          :to client-a
                          :highest-sequence-delivered 0
                          :highest-sequence-to-send 1
                          :full? false}
          (<?!! to-b) => {:intent :receive :from client-a :to client-b :sequence 1 :payload "bar"})))))
