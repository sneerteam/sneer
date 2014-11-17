(ns sneer.server.router-test
  (:require
   [clojure.core.async :as async :refer [alt! chan >! <! >!! <!! alts!! timeout]]
   [midje.sweet :refer :all]
   [sneer.test-util :refer :all]
   [sneer.async :refer :all]
   [sneer.commons :refer [produce!]]
   [sneer.server.router :as router]))

(def ^:private empty-q clojure.lang.PersistentQueue/EMPTY)

(defn dropping-conj [max-size xs x]
  (let [full? (>= (count xs) max-size)]
    (if full? xs (conj xs x))))

(defn dropping-enqueue [max-size q element]
  (dropping-conj max-size (or q empty-q) element))

(defn pop-only [xs x]
  (if (= x (peek xs))
    (pop xs)
    xs))

(defprotocol Router
  (enqueue! [_ sender receiver tuple]
    "Adds tuple to its receiver/sender send queue if the queue isn't full. Returns whether the queue was able to accept tuple (queue was not full).")
  (peek-tuple-for [_ receiver]
    "Returns the next tuple to be sent to receiver.")
  (pop-tuple-for! [_ receiver]
    "Removes the next tuple to be sent to receiver from its queue. If the queue had been full and is now empty, returns the from-puk of the sender to be notified."))

(defn create-router [queue-size]
  (let [enqueue (partial dropping-enqueue queue-size)
        qs (atom {})]
    (reify Router
      (enqueue! [_ sender receiver tuple]
        (let [qs-before @qs]
        #_(swap! qs update-in [receiver sender] enqueue tuple)
          (swap! qs update-in [receiver       ] enqueue tuple)
          (not (identical? qs qs-before))))
      (peek-tuple-for [_ receiver]
        (peek (@qs receiver)))
      (pop-tuple-for! [_ receiver]
        ))))

#_(defn start-router [in out queue-size send-retry-timeout-fn]
   "in - channel with:
     {:enqueue-packet-to-forward packet} where packet: {:from puk :to puk :tuple t}
     {:dequeue-packet-sent       packet} where packet: {:from puk :to puk :tuple t}
   out - channel for:
     {:enqueue-result boolean :packet packet} where packet: {:from puk :to puk :tuple t}
     {:next-packet-to-send packet}            where packet: {          :to puk :tuple t}"
   (let [enqueue (fn [q tuple] dropping-enqueue queue-size q tuple)]
     (go-trace
       (loop [qs {} ; {to {from q}} where q: PersistentQueue of tuple.
              receivers [] ]
         (alt!
           in
           ([command]
             (recur
               (match command
                 {:enqueue-packet-to-forward packet}
              
                 (let [{:keys [from to tuple]} packet
                       new-qs (update-in qs [to from] enqueue tuple)
                       accepted? (not (identical? new-qs qs))]
                   (>! out {:enqueue-result accepted? :packet packet})
                   new-qs)
              
                 {:dequeue-packet-sent {:from from :to to :tuple tuple}}
                 (update-in qs [to from] pop-only tuple)))
             (when new-tuple-arrived-at-first-queue-position (>! out {:next-packet-to-send packet}))
             )
      
           [packets-to-send packet | :none ]
      
           )))))

(facts
  "Routing"

  (let [q-size 3
        router (create-router q-size)]
    
    (fact "Queues start empty and accept tuples."
      (enqueue! router :A :B "Hello") => true)

    (fact "One value is routed from A to B"
      (peek-tuple-for router :B) => "Hello")

    (fact "One value is routed from B to A"
      (enqueue! router :B :A "Hi there")
      (peek-tuple-for router :A) => "Hi there")
    ))