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


(def online-count (constantly 20))

(defn- next-packet-to-send [online-clients send-round]
  (let [client (peek send-round)]
    (when-some [packet (get-in online-clients [client :pending-to-send])]
      (assoc packet :to client))))

(def tuple-signature (juxt :author :id))

(defn- ack [state from signature]
  (let [pending (get-in state [:online-clients from :pending-to-send :send])]
    (if (= signature (tuple-signature pending))
      (let [router (-> state :router (pop-packet-for from))]
        (-> state
          (assoc :router router)
          (update-in [:online-clients from              :pending-to-send] (constantly (peek-packet-for router from)))
          (update-in [:online-clients (:author pending) :pending-to-send] (constantly (peek-packet-for router (:author pending))))
          ))
      state)))

; [{:keys [packets-in packets-out router online-clients send-round resend-timer] :as state}]

(defn- send-op [{:keys [packets-out online-clients send-round resend-timeout]}]
  (if-some [packet (next-packet-to-send online-clients send-round)]
    [packets-out packet]
    resend-timeout))

(defn- channel-ops [{:keys [packets-in] :as state}]
  #_(println "Send-op: " (send-op state))
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
            state (update-in state [:online-clients from :pending-to-send] #(if % % (peek-packet-for router from)))]
        (match packet
          {:send tuple :to to}
          (let [packets-out (:packets-out state)]
            (if (queue-full? router from to)
              (do
                (>!! packets-out {:nak (:id tuple) :for to :to from})
                state)
              (do
                (>!! packets-out {:ack (:id tuple) :for to :to from})
                (update-in state [:router] enqueue! from to tuple))))

          {:ack author :id id}
          (ack state from [author id])

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
    #_(println "Chosen:" chosen)
    (apply handle state
      chosen)))

(defn start-connector [queue-size packets-in packets-out resend-timeout-fn]
  (thread
    (loop-state -iterate
                {:packets-in (NamedChannel. :packets-in packets-in)
                 :packets-out (NamedChannel. :packets-out packets-out)
                 :router (create-router queue-size)
                 :online-clients {}
                 :send-round empty-queue
                 :resend-timeout-fn resend-timeout-fn
                 :resend-timeout (->named resend-timeout-fn)} )
    (close! packets-out)))
