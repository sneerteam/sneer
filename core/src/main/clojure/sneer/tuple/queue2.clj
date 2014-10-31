(ns sneer.tuple.queue2
  (:require [sneer.commons :refer [produce!]]
            [sneer.core :refer [query-tuples]]
            [sneer.rx :refer [observe-for-io]]
            [sneer.async :refer [go-trace]]
            [rx.lang.clojure.core :as rx]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async :refer [<! >! >!! <!! chan go-loop alt!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Reference from server protocol

#_(defn status-to [to follower state]
   {:intent :status-of-queues
    :to to
    :follower follower
    :highest-sequence-delivered (:highest-sequence-delivered state)
    :highest-sequence-to-send (highest-sequence-to-send state)
    :full? false})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Store

(def reliable-client-tables
  [
   {:table :follower
    :columns [:id :int :autoincrement
              :puk :blob :unique
              :next-sequence-to-send :int]}

   {:table :follower-queue
    :columns [[:sequence :int :autoincrement]
              [:follower :int]
              [:tuple :int]]}])

(defprotocol QueueStore
  (-empty? [store to])
  (-peek [store to])
  (-enqueue [store to tuple])
  (-pop [store to])
  (-reset [store to])
  (-unreset [store to])
  (-reset? [store to]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Logic

(defprotocol Queue
  (-enqueue-to-send [queue tuple])
  (-packet-to-send [queue])
  (-handle-packet-from-server [queue packet]))

(def empty-q clojure.lang.PersistentQueue/EMPTY)

(def initial-state
  {:sequence 0
   :to-send empty-q
   :sent empty-q})

(defn enqueue-to-send [state payload]
  (update-in state [:to-send] conj payload))

(defn- pop-packet [state]
  (-> state
    (update-in [:to-send] pop)
    (update-in [:sent] conj (-> state :to-send first))
    (update-in [:sequence] inc)))

(defn- reset [{:keys [sequence to-send sent] :as state}]
  {:sequence (- sequence (count sent))
   :sent empty-q
   :to-send (into sent to-send)
   :reset true})

(defn- unreset [state]
  (dissoc state :reset))

(defn- handle-delivery
" sent      to-send
  (5 6 7 8) (9 10 11)
     D       S          ; Keep 7 and 8

  sent      to-send
  (5 6 7 8) (9 10 11)
       D     S          ; Keep 8"
  [{:keys [sequence sent] :as state} highest-sequence-delivered]
  (let [undelivered (- sequence highest-sequence-delivered 1)]
    (assoc state :sent (into empty-q (take-last undelivered sent)))))

(defn handle-packet-from-server [state packet]
  (match packet
         {:highest-sequence-to-send   hsts
          :highest-sequence-delivered hsd}
           (case (- (state :sequence) hsts)
             0 (-> state unreset pop-packet (handle-delivery hsd))
             1 (-> state unreset            (handle-delivery hsd))
             (reset state))))


(defn packet-to-send [state from to]
  (when-some [payload (-> state :to-send first)]
    (merge (select-keys state [:sequence :reset])
      {:intent :send
       :from from
       :to to
       :payload payload})))

(defn create [_store from to]
  (let [state (atom initial-state)]
    (reify Queue
      (-enqueue-to-send [queue tuple]
        (swap! state enqueue-to-send tuple)
        queue)
      (-packet-to-send [queue]
        (packet-to-send @state from to))
      (-handle-packet-from-server [queue packet]
        (swap! state handle-packet-from-server packet)
        queue))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Wiring

(def IMMEDIATELY (doto (async/chan)
                   async/close!))

(def NEVER (async/chan))

(defn new-retry-timeout []
  (async/timeout 3000))

(defn start-queue-transmitter [store from to tuples-in packets-in packets-out]
  (let [queue (create store from to)]
    (go-trace
      (loop [previous-packet nil
             previous-time-to-send nil]
        (let [packet (-packet-to-send queue)
              time-to-send (cond
                             (nil? packet) NEVER
                             (= packet previous-packet) previous-time-to-send
                             :else IMMEDIATELY)]
          (alt! :priority true
          
            time-to-send
            ([_] (do
                   (>! packets-out packet)
                   (recur packet (new-retry-timeout))))
          
            tuples-in
            ([tuple]
              (when tuple
                (-enqueue-to-send queue tuple)
                  (recur packet time-to-send)))
          
            packets-in
            ([packet]
              (-handle-packet-from-server queue packet)
              (recur packet time-to-send)))))
      
      (async/close! packets-out))))
