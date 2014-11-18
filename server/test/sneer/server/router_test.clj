(ns sneer.server.router-test
  (:require
   [clojure.set :refer [difference]]
   [midje.sweet :refer :all]
   [sneer.test-util :refer :all]
   [sneer.async :refer :all]
   [sneer.commons :refer [produce!]]
   [sneer.server.router :as router]))

(def ^:private empty-q clojure.lang.PersistentQueue/EMPTY)

(defn dropping-conj [xs x max-size]
  (let [full? (>= (count xs) max-size)]
    (if full? xs (conj xs x))))

(defn dropping-enqueue [q element max-size]
  (dropping-conj (or q empty-q) element max-size))

(defn pop-only [xs x]
  (if (= x (peek xs))
    (pop xs)
    xs))

(defn- next-wrap [coll element]
  (let [count (count coll)]
    (if (zero? count)
      nil
      (let [vec (vec coll)
            index (.indexOf vec element)]
        (get vec (mod (inc index) count))))))

(defprotocol Router
  (enqueue! [_ sender receiver tuple]
    "Adds tuple to its receiver/sender send queue if the queue isn't full. Returns whether the queue was able to accept tuple (queue was not full).")
  (peek-tuple-for [_ receiver]
    "Returns the next tuple to be sent to receiver.")
  (pop-tuple-for! [_ receiver]
    "Removes the next tuple to be sent to receiver from its queue. If the queue had been full and is now empty, returns the sender to be notified."))

(defn- peek-for [receiver-q]
  (let [{:keys [qs-by-sender turn]} receiver-q]
    (when qs-by-sender
      (peek (qs-by-sender turn)))))

(defn- enqueue-for [receiver-q queue-size sender tuple]
  (let [before receiver-q
        receiver-q (update-in before [:qs-by-sender sender] dropping-enqueue tuple queue-size)]
    (if-some [turn (:turn before)]
      receiver-q
      (assoc receiver-q :turn sender))))

(defn- pop-tuple [receiver-q]
  (let [turn (:turn receiver-q)
        receiver-q (update-in receiver-q [:qs-by-sender turn] pop)
        sender-q-empty? (nil? (peek-for receiver-q))
        senders (-> receiver-q :qs-by-sender keys)
        receiver-q (assoc receiver-q :turn (next-wrap senders turn))]
    (if sender-q-empty?
      (update-in receiver-q [:qs-by-sender] dissoc turn) ; If there was only one sender, :turn will point to it (removed sender) but that's ok because receiver-q will be empty and will be removed from qs. 
      receiver-q)))

(defn- pop-tuple-for [qs receiver]
  (let [qs (update-in qs [receiver] pop-tuple)
        empty? (-> qs receiver peek-for nil?)]
    (if empty?
      (dissoc qs receiver)
      qs)))

(defn- senders-to-notify-of [qs receiver]
  (get-in qs [receiver :senders-to-notify-when-empty]))

(defn- mark-full [receiver-q sender]
  (update-in receiver-q [:senders-to-notify-when-empty] #(conj (or % #{}) sender)))

(defn create-router [queue-size]
  (let [qs (atom {})] ; { receiver { :qs-by-sender                 { sender q }
                      ;              :turn                         sender
                      ;              :senders-to-notify-when-empty #{sender} } } 
    (reify Router
      (enqueue! [_ sender receiver tuple]
        (let [qs-before @qs
              qs-after (swap! qs update-in [receiver] enqueue-for queue-size sender tuple)
              full? (identical? qs-after qs-before)]
          (when full?
            (swap! qs update-in [receiver] mark-full sender))
          (not full?)))
      (peek-tuple-for [_ receiver]
        (peek-for (@qs receiver)))
      (pop-tuple-for! [_ receiver]
        (let [to-notify #(get-in @qs [receiver :senders-to-notify-when-empty])
              to-notify-before (to-notify)]
          (swap! qs pop-tuple-for receiver)
          (-> (difference to-notify-before (to-notify)) first))))))


(facts
  "Routing"

  (let [max-q-size 3
        subject (atom nil)
        reset! #(reset! subject (create-router max-q-size))
        enq! (fn [from to msg] (enqueue! @subject from to msg))
        peek #(peek-tuple-for @subject %)
        pop! #(do (pop-tuple-for! @subject %) (peek %))
        pop? #(pop-tuple-for! @subject %)]
    
    (reset!)
    (fact "Queues start empty and accept tuples."
      (peek :B) => nil
      (enq! :A :B "Hello") => true)

    (fact "One value is routed from A to B"
      (peek :B) => "Hello")

    (fact "One value is routed from B to A"
      (enq! :B :A "Hi there")
      (peek :A) => "Hi there")

    (fact "Tuples are enqueued."
      (enq! :A :B "Hello Again")
      (peek :B) => "Hello"
      (pop! :B) => "Hello Again")

    (fact "Tuples grow up to max-size."
      (enq! :A :B "Msg 2")
      (enq! :A :B "Msg 3") => true
      (enq! :A :B "Msg 4") => false)
    
    (fact "Tuples from multiple senders are multiplexed."
      (enq! :C :B "Hello  from C")
      (enq! :C :B "Hello2 from C")
      (pop! :B) => "Hello  from C"
      (pop! :B) => "Msg 2"
      (pop! :B) => "Hello2 from C"
      (pop! :B) => "Msg 3")
  
    (reset!)
    (fact "Blank crazy pop doesn't crash"
      (pop! :Foo) => nil)

    (reset!)
    (fact "Queues that become empty return nil."
      (enq! :A :B "A1")
      (enq! :C :B "C1")
      (enq! :C :B "C2")
      (peek :B) => "A1"
      (pop! :B) => "C1"
      (pop! :B) => "C2"
      (pop! :B) => nil)

    (reset!)
    (fact "Multiple receivers can have enqueued tuples."
      (enq! :A :B "AB1")
      (enq! :A :C "AC1")
      (enq! :B :A "BA1")
      (enq! :B :C "BC1")
      (peek :A) => "BA1"
      (peek :B) => "AB1"
      (peek :C) => "AC1"
      (pop! :A) => nil
      (pop! :B) => nil
      (pop! :C) => "BC1"
      (pop! :C) => nil)
    
    (reset!)
    (fact "Senders are notified of queues that were full and became empty."
      (enq! :A :B "AB1")
      (enq! :A :B "AB2")
      (enq! :A :B "AB3")
      (enq! :A :B "AB4") => false
      (pop? :B) => nil
      (pop? :B) => nil
      (pop? :B) => :A
      (pop? :B) => nil)))
