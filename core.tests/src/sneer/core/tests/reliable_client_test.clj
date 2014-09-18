(ns sneer.core.tests.reliable-client-test
  (:require [midje.sweet :refer :all]
            [clojure.core.match :refer [match]]))

(def empty-q clojure.lang.PersistentQueue/EMPTY)

(defn create []
  {:sequence 0
   :to-send empty-q
   :sent empty-q})

(defn enqueue-to-send [state payload]
  (update-in state [:to-send] conj payload))

(defn- pop-packet [state]
  (-> state
    (update-in [:sent] conj (-> state :to-send first))
    (update-in [:to-send] pop)
    (update-in [:sequence] inc)))

(defn- reset [{:keys [sequence to-send sent] :as state}]
  {:sequence (- sequence (count sent))
   :sent empty-q
   :to-send (into sent to-send)
   :reset true})


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
             0 (-> state pop-packet (handle-delivery highest-sequence-delivered))
             1 (-> state            (handle-delivery highest-sequence-delivered))
             (reset state))))


(defn packet-to-send [state]
  (when-some [payload (-> state :to-send first)]
    (assoc (select-keys state [:sequence :reset]) :payload payload)))


(facts
 "New Queues"

  (fact "A new queue has no packet to send."
    (-> (create) packet-to-send) => nil)

  ; TODO: Delete these tests that break the queue's encapsulation using pop-packet.
  
  (fact "Packet enqueing is FIFO."
    (let [queue (-> (create) (enqueue-to-send :foo) (enqueue-to-send :bar))]
      (->> queue packet-to-send) => {:sequence 0 :payload :foo}
      (->> queue pop-packet packet-to-send) => {:sequence 1 :payload :bar}))
  
  (fact "Every new packet gets a new sequence number."
  (let [queue (-> (create) (enqueue-to-send :foo))]
    (-> queue pop-packet (enqueue-to-send :bar) packet-to-send :sequence) => 1)))


(facts "Packet Handling"

  (let [queue (-> (create) (enqueue-to-send :foo) (enqueue-to-send :bar))]

    (fact "Packet enqueing is FIFO."
      (let [queue (-> (create) (enqueue-to-send :foo) (enqueue-to-send :bar))
            simulate (fn [highest-sequence-to-send]
                       (-> queue
                         (handle-packet-from-server
                           {:intent :status-of-queues
                            :highest-sequence-delivered -1
                            :highest-sequence-to-send highest-sequence-to-send
                            :full? false})
                         packet-to-send))]
    
      (simulate -1) => {:sequence 0 :payload :foo}
      (simulate 0)  => {:sequence 1 :payload :bar}
      (simulate 42) => {:sequence 0 :payload :foo :reset true}))
  
    (fact "Delivered packets are forgotten."
       (let [queue (handle-packet-from-server
                     queue
                     {:intent :status-of-queues
                      :highest-sequence-delivered -1
                      :highest-sequence-to-send 0
                      :full? false})
             queue (handle-packet-from-server
                     queue
                     {:intent :status-of-queues
                      :highest-sequence-delivered -1
                      :highest-sequence-to-send -1  ; Restarted
                      :full? false})]
         (packet-to-send queue) => {:sequence 0 :payload :foo :reset true})
         
       (let [queue (handle-packet-from-server
                     queue
                     {:intent :status-of-queues
                      :highest-sequence-delivered 0  ; Delivered
                      :highest-sequence-to-send 0
                      :full? false})
             queue (handle-packet-from-server
                     queue
                     {:intent :status-of-queues
                      :highest-sequence-delivered -1
                      :highest-sequence-to-send -1  ; Restarted
                      :full? false})]
         (packet-to-send queue) => {:sequence 1 :payload :bar :reset true})
       ))
    
  (let [enqueue (fn [queue start count]
                  (reduce enqueue-to-send queue (range start (+ start count))))
        scenario (fn [enq1 hsts1 hsd1 full?1 enq2 hsts2 hsd2 full?2 seq reset fact]
                   (let [queue (create)
                         queue (enqueue queue 0 enq1)
                         queue ()]
                     #_(fact (str fact " - Packet payload must be equal to sequence number (this test is designed that way).")
                        payload => seq)
                     ))]

    ;              1st from Server        2nd from Server   peek-to-send
    ;        enq   hsts hsd full?   enq   hsts hsd full?    seq reset    fact
    (scenario  0    nil nil   nil     0    nil nil   nil    nil   nil    "A new queue has no packet to send.")
    (scenario  1    nil nil   nil     0    nil nil   nil      0   nil    "A packet can be enqueued to send.")
    (scenario  2    nil nil   nil     0    nil nil   nil      0   nil    "Enqueueing is FIFO.")
    (scenario  1     -1  -1 false     0    nil nil   nil      0   nil    "When the server has no packets sent (initial server state), queue sends first packet.")
    (scenario  1      0  -1 false     0    nil nil   nil    nil   nil    "Server sending a packet pops it from the queue (with one enqueued).")
    (scenario  2      0  -1 false     0    nil nil   nil      1   nil    "Server sending a packet pops it from the queue (with more enqueued).")
    (scenario  1      0  -1 false     0     -1  -1 false      0  true    "Undelivered packets are sent when the server restarts.")
    (scenario  1      0   0 false     0     -1  -1 false    nil   nil    "Delivered packets are forgotten (with one enqueued).")
    (scenario  2      0   0 false     0     -1  -1 false      1  true    "Delivered packets are forgotten (with more enqueued).")
    ;TODO:
    ;  Multiple packets.
    ;  Inconsistent packets from server.
    ))

