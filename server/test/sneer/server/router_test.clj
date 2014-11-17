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

(defn enqueue-for [receiver-q enqueue-fn sender tuple]
  (let [after (update-in receiver-q [:qs-by-sender sender] enqueue-fn tuple)]
    (if-some [turn (:turn receiver-q)]
      after
      (assoc after :turn sender))))

(defn next-turn [receiver-q]
  (let [senders (-> receiver-q :qs-by-sender keys vec)
        turn (:turn receiver-q)
        index (.indexOf senders turn)]
    (get senders (mod (inc index) (count senders)))))

(defn pop-tuple [receiver-q]
  (let [after (update-in receiver-q [:qs-by-sender (:turn receiver-q)] pop)]
    (if (identical? after receiver-q)
      after
      (assoc after :turn (next-turn receiver-q)))))

(defn create-router [queue-size]
  (let [enqueue-fn (partial dropping-enqueue queue-size)
        qs (atom {})]
    (reify Router
      (enqueue! [_ sender receiver tuple]
        (let [qs-before @qs
              qs-after (swap! qs update-in [receiver] enqueue-for enqueue-fn sender tuple)]
          (not (identical? qs-after qs-before))))
      (peek-tuple-for [_ receiver] ; { :qs-by-sender sender->q  :turn sender }
        (let [qs @qs
              {:keys [qs-by-sender turn]} (qs receiver)]
          (peek (qs-by-sender turn))))
      (pop-tuple-for! [_ receiver]
        (swap! qs update-in [receiver] pop-tuple)))))


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

    (fact "Tuples are enqueued."
      (enqueue! router :A :B "Hello Again")
      (peek-tuple-for router :B) => "Hello"
      (pop-tuple-for! router :B)
      (peek-tuple-for router :B) => "Hello Again")

    (fact "Tuples grow up to max-size."
      (enqueue! router :A :B "Msg 2")
      (enqueue! router :A :B "Msg 3") => true
      (enqueue! router :A :B "Msg 4") => false)
    
    (fact "Tuples from multiple senders are multiplexed."
     (enqueue! router :C :B "Hello  from C")
     (enqueue! router :C :B "Hello2 from C")
     (pop-tuple-for! router :B)
     (peek-tuple-for router :B) => "Hello  from C"
     (pop-tuple-for! router :B)
     (peek-tuple-for router :B) => "Msg 2"
     (pop-tuple-for! router :B)
     (peek-tuple-for router :B) => "Hello2 from C"
     (pop-tuple-for! router :B)
     (peek-tuple-for router :B) => "Msg 3")))
