(ns sneer.server.router-connector
  (:require
    [clojure.core.async :as async :refer [to-chan chan close! go <!! >!! thread alts!!]]
    [clojure.core.async.impl.protocols :as impl]
    [clojure.core.match :refer [match]]
    [sneer.test-util :refer :all]
    [sneer.async :refer :all]
    [sneer.commons :refer [empty-queue loop-state]]
    [sneer.server.router :refer :all]))

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

(def online-count (constantly 20))

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

(defn- update-pending-to-send [state client]
  (assoc-in state [:online-clients client :pending-to-send] (peek-packet-for (:router state) client)))

(defn- handle-ack [state from signature]
  (let [pending (get-in state [:online-clients from :pending-to-send])]
    (if (= signature (packet-signature pending))
      (let [router (-> state :router (pop-packet-for from))
            peer (first signature)]
        (-> state
            (assoc :router router)
            (update-pending-to-send from)
            (update-pending-to-send peer)))
      state)))

(defn- handle-send [state from to tuple]
  (let [packets-out (:packets-out state)]
    (if (queue-full? (:router state) from to)
      (do
        (>!! packets-out {:nak (id-of tuple) :for to :to from})
        state)
      (do
        (>!! packets-out {:ack (id-of tuple) :for to :to from})
        (-> state
          (update-in [:router] enqueue! from to tuple)
          (update-pending-to-send to))))))

(defn- send-op [{:keys [packets-out online-clients send-round resend-timeout]}]
  (if-some [packet (next-packet-to-send online-clients send-round)]
    [packets-out packet]
    resend-timeout))

(defn- channel-ops [{:keys [packets-in] :as state}]
  [(send-op state)
   packets-in
   #_offline-timer])

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
      (let [router (:router state)
            state (update-in state [:online-clients from :online-countdown] online-count)
            state (update-pending-to-send state from)]
        (match packet
          {:send tuple :to to}
          (handle-send state from to tuple)
          
          {:ack author}
          (handle-ack state from [author (:id packet)])

          :else
          state)))
    :break))

(defmethod handle :packets-out [state packet _]
  (update-in state [:send-round] pop))

(defmethod handle :resend-timeout [{:keys [online-clients resend-timeout-fn]} _ _]
  {:send-round (send-round online-clients)
   :resend-timeout (->named resend-timeout-fn)})

(defn- -iterate [state]
  (let [chosen (alts!! (channel-ops state) :priority :true)]
    (apply handle state
      chosen)))

(defn start-connector [queue-size packets-in packets-out & [resend-timeout-fn]]
  (let [resend-timeout-fn (or resend-timeout-fn #(async/timeout 100))]
    (thread
      (loop-state -iterate
                  {:packets-in        (NamedChannel. :packets-in packets-in)
                   :packets-out       (NamedChannel. :packets-out packets-out)
                   :router            (create-router queue-size)
                   :online-clients    {}
                   :send-round        empty-queue
                   :resend-timeout-fn resend-timeout-fn
                   :resend-timeout    (->named resend-timeout-fn)})
      (close! packets-out))))
