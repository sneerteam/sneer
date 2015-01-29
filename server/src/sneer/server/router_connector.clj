(ns sneer.server.router-connector
  (:require
    [clojure.core.async :as async :refer [to-chan chan close! go <!! >!! thread alts!!]]
    [clojure.core.async.impl.protocols :as impl]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.commons :refer [empty-queue loop-state]]
    [sneer.server.prevalence :as p]
    [sneer.server.router :refer :all]))

(def resend-timeout-millis 500)
(def online-count 20)

(defrecord NamedChannel [name channel]
  impl/ReadPort
  (take! [_ fn]
    (impl/take! channel fn))

  impl/WritePort
  (put! [_ value fn]
    (impl/put! channel value fn))

  Object
  (toString [_]
    (str "Ch" name)))

(defmethod print-method NamedChannel
  [nc writer]
  (.write writer (str nc)))

(defmethod print-method clojure.lang.PersistentQueue
  [q writer]
  (.write writer (if-some [seq (seq q)] (str seq) "nil")))

(defn- next-packet-to-send [online-clients send-round]
  (when-some [client (peek send-round)]
    (when-some [packet (get-in online-clients [client :pending-to-send])]
      (assoc packet :to client))))

(defn- author-of [tuple]
  (get tuple "author"))

(defn- id-of [tuple]
  (get tuple "id"))

(defn- packet-signature [packet]
  (if-let [tuple (:send packet)]
    [(author-of tuple) (id-of tuple)]
    [(:cts packet)     nil          ]))

(defn- online? [state client]
  (get-in state [:online-clients client]))

(defn- update-pending-to-send [state client]
  (if (online? state client)
    (assoc-in state [:online-clients client :pending-to-send]
      (peek-packet-for @(:router state) client))
    state))

(defn- handle-ack [state from signature]
  (let [pending (get-in state [:online-clients from :pending-to-send])]
    (if (= signature (packet-signature pending))
      (let [peer (first signature)]
        (p/handle! (:router state) [:pop-packet-for from])
        (-> state
            (update-pending-to-send from)
            (update-pending-to-send peer)))
      state)))

(defn- reply [packets-out n|ack from to tuple]
  (>!! packets-out {n|ack (id-of tuple) :for to :to from}))

(defn- send-gcm-if-necessary! [state router to]
  (when-not (get-in state [:online-clients to])
    (when-not (peek-packet-for router to)
      (>!! (:gcm-out state) to))))

(defn- handle-send [state from to tuple]
  (let [router (:router state)
        packets-out (:packets-out state)]
    (if (duplicated-tuple? @router from to tuple)
      (do
        (reply packets-out :ack from to tuple)
        state)
      (if (queue-full? @router from to)
        (do
          (reply packets-out :nak from to tuple)
          state)
        (do
          (send-gcm-if-necessary! state @router to)
          (p/handle! router [:enqueue from to tuple])
          (reply packets-out :ack from to tuple)
          (-> state
            (update-pending-to-send to)))))))

(defn- send-op [{:keys [packets-out online-clients send-round resend-timeout]}]
  (if-some [packet (next-packet-to-send online-clients send-round)]
    [packets-out packet]
    resend-timeout))

(defn- channel-ops [{:keys [packets-in] :as state}]
  [(send-op state)
   packets-in])

(defn- ->named [resend-timeout-fn]
  (NamedChannel. :resend-timeout (resend-timeout-fn)))

(defn- send-round [online-clients]
  (->>
    online-clients
    keys
    (filter #(get-in online-clients [% :pending-to-send]))
    (into empty-queue)))

(defmulti handle (fn [_ _ channel] (:name channel)))

(defmethod handle :packets-in [state packet _]
  (if packet
    (when-some [from (:from packet)]
      (let [state (assoc-in state [:online-clients from :online-countdown] online-count)
            state (update-pending-to-send state from)]
        (match packet
          {:send tuple :to to}
          (handle-send state from to tuple)
          
          {:ack author}
          (handle-ack state from [author (:id packet)])

          :else
          state)))
    :break))

(defmethod handle :packets-out [state _ _]
  (let [client (-> state :send-round peek)
        state' (-> state
                 (update-in [:online-clients client :online-countdown] dec)
                 (update-in [:send-round] pop))]
    (if (zero? (get-in state' [:online-clients client :online-countdown]))
      (do
        (>!! (:gcm-out state') client)
        (update-in state' [:online-clients] dissoc client))
      state')))

(defmethod handle :resend-timeout [{:keys [online-clients resend-timeout-fn]} _ _]
  {:send-round (send-round online-clients)
   :resend-timeout (->named resend-timeout-fn)})

(defn- -iterate [state]
  (let [chosen (alts!! (channel-ops state) :priority :true)]
    (apply handle state
      chosen)))

(defmulti  handle-event (fn [_ [type]] type))
(defmethod handle-event :pop-packet-for [router [_ from]]
  (pop-packet-for router from))
(defmethod handle-event :enqueue [router [_ from to tuple]]
  (enqueue router from to tuple))

(defn- start [prevalent-router packets-in packets-out resend-timeout-fn gcm-out]
  (thread
    (loop-state -iterate
                {:packets-in        (NamedChannel. :packets-in packets-in)
                 :packets-out       (NamedChannel. :packets-out packets-out)
                 :router            prevalent-router
                 :online-clients    {}
                 :send-round        empty-queue
                 :resend-timeout-fn resend-timeout-fn
                 :resend-timeout    (->named resend-timeout-fn)
                 :gcm-out           gcm-out})
    (close! packets-out)
    (close! gcm-out)))

(defn start-connector [prevalence-file packets-in packets-out & [gcm-out]]
  (let [gcm-out (or gcm-out (dropping-chan))]
    (start (p/prevayler-jr! handle-event
                            (create-router 200)
                            prevalence-file)
           packets-in
           packets-out
           gcm-out
           #(async/timeout resend-timeout-millis))))

(defn start-transient-connector [queue-size packets-in packets-out resend-timeout-fn & [gcm-out]]
  (let [gcm-out (or gcm-out (dropping-chan))]
    (start (p/prevayler-jr! handle-event (create-router queue-size))
           packets-in
           packets-out
           resend-timeout-fn
           gcm-out)))
