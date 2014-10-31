(ns sneer.tuple.reliable-client-test
  (:require [midje.sweet :refer :all]
            [clojure.core.match :refer [match]]))

; (do (require 'midje.repl) (midje.repl/autotest))

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
         {:highest-sequence-to-send   highest-sequence-to-send
          :highest-sequence-delivered highest-sequence-delivered}
           (case (- (state :sequence) highest-sequence-to-send)
             0 (-> state unreset pop-packet (handle-delivery highest-sequence-delivered))
             1 (-> state unreset            (handle-delivery highest-sequence-delivered))
             (reset state))))


(defn packet-to-send [state]
  (when-some [payload (-> state :to-send first)]
    (assoc (select-keys state [:sequence :reset]) :payload payload)))

;(reify QueueStore
;      (-empty? [_ to]
;        (-> @state :q empty?))
;      (-peek [_ to]
;        (-> @state :q peek))      
;      (-enqueue [_ to tuple]
;        (swap! state enqueue tuple))
;      (-pop [_ to]
;        (swap! state update-in [:q] pop)))

(tabular "Packet Handling"

  (fact 
    (let [enqueue (fn [queue start count]
                    (reduce enqueue-to-send queue (range start (+ start count))))
          simulate-from-server (fn [queue highest-sequence-to-send highest-sequence-delivered full?]
                                 (if highest-sequence-to-send
                                   (handle-packet-from-server
                                     queue
                                     {:intent :status-of-queues
                                      :highest-sequence-to-send highest-sequence-to-send
                                      :highest-sequence-delivered highest-sequence-delivered
                                      :full? full?})
                                   queue))
          queue initial-state
          queue (enqueue queue     0 ?enq1)
          queue (simulate-from-server queue ?hsts1 ?hsd1 ?full?1)
          queue (enqueue queue ?enq1 ?enq2)
          queue (simulate-from-server queue ?hsts2 ?hsd2 ?full?2)
          packet-to-send (packet-to-send queue)]
      (:sequence packet-to-send) => ?seq
      (:payload packet-to-send) => ?seq ;This test is designed so that the sequence and the payload are always the same.
      (:reset packet-to-send) => ?reset))
  
    ?enq1 ?hsts1 ?hsd1 ?full?1 ?enq2 ?hsts2 ?hsd2 ?full?2   ?seq ?reset   ?obs
        0    nil   nil     nil     0    nil   nil     nil    nil    nil   "A new queue has no packet to send."
        1    nil   nil     nil     0    nil   nil     nil      0    nil   "A packet can be enqueued to send."
        2    nil   nil     nil     0    nil   nil     nil      0    nil   "Enqueueing is FIFO."
        1     -1    -1   false     0    nil   nil     nil      0    nil   "When the server has no packets sent (initial server state), queue sends first packet."
        1      0    -1   false     0    nil   nil     nil    nil    nil   "Server sending a packet pops it from the queue (with one enqueued)."
        3      0    -1   false     0      1    -1   false      2    nil   "Server sending a packet pops it from the queue (with three enqueued)."
        1     42     0   false     0    nil   nil     nil      0   true   "Reset is sent when server gets out of sync."
        2     42     0   false     0     -1    -1   false      0    nil   "Reset is not needed for happy-day sequencing."
        1      0    -1   false     0     -1    -1   false      0   true   "Undelivered packets are sent when the server restarts."
        1      0     0   false     0     -1    -1   false    nil    nil   "Delivered packets are forgotten (with one enqueued)."
        2      0     0   false     0     -1    -1   false      1   true   "Delivered packets are forgotten (with two enqueued)."
        7      0     0   false     0     -1    -1   false      1   true   "Delivered packets are forgotten (with several enqueued).")
