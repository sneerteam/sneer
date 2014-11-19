(ns sneer.server.router
  (:require
   [clojure.set :refer [difference]]))

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

(defn next-wrap [coll element]
  (let [count (count coll)]
    (if (zero? count)
      nil
      (let [vec (vec coll)
            index (.indexOf vec element)]
        (get vec (mod (inc index) count))))))

(defn- peek-for [receiver-q]
  (let [{:keys [qs-by-sender turn]} receiver-q]
    (when qs-by-sender
      (peek (qs-by-sender turn)))))

(defn- mark-full [receiver-q sender]
  (update-in receiver-q [:senders-to-notify-when-empty] (fnil conj #{}) sender))

(defn- sender-queue-count [receiver-q sender]
  (-> receiver-q (get-in [:qs-by-sender sender]) count))

(defn sender-queue-full? [receiver-q sender max-queue-size]
  "Returns whether the receiver/sender send queue is full."
  (>= (sender-queue-count receiver-q sender) max-queue-size))


(defn queue-full? [router sender receiver]
  "Returns whether the receiver/sender send queue is full."
  (sender-queue-full? (router receiver) sender (router :max-queue-size)))
      

(defn- enqueue-for [receiver-q queue-size sender tuple]
  (let [turn (:turn receiver-q)
        receiver-q (update-in receiver-q [:qs-by-sender sender] dropping-enqueue tuple queue-size)
        receiver-q (if turn
                     receiver-q
                     (assoc receiver-q :turn sender))]
    
    (if (sender-queue-full? receiver-q sender queue-size)
      (mark-full receiver-q sender)
      receiver-q)))

(defn- pop-tuple [receiver-q]
  (let [turn (:turn receiver-q)
        receiver-q (update-in receiver-q [:qs-by-sender turn] pop)
        sender-q-empty? (nil? (peek-for receiver-q))
        senders (-> receiver-q :qs-by-sender keys)
        receiver-q (assoc receiver-q :turn (next-wrap senders turn))]
    (if sender-q-empty?
      (-> receiver-q
        (update-in [:qs-by-sender] dissoc turn)
        (update-in [:senders-to-notify-when-empty] disj turn)) ; If there was only one sender, :turn will point to it (removed sender) but that's ok because receiver-q will be empty and will be removed from qs. 
      receiver-q)))

(defn- pop-tuple-for [qs receiver]
  (let [qs (update-in qs [receiver] pop-tuple)
        empty? (-> qs receiver peek-for nil?)]
    (if empty?
      (dissoc qs receiver)
      qs)))

(defn- senders-to-notify-of [receiver qs]
  (get-in qs [receiver :senders-to-notify-when-empty]))



(defn enqueue! [router sender receiver tuple]
  "Adds tuple to its receiver/sender send queue. Pre-requisite: the queue is not full."
  (update-in router [receiver] enqueue-for (router :max-queue-size) sender tuple))
      
(defn peek-tuple-for [router receiver]
  "Returns the next tuple to be sent to receiver."
  (peek-for (router receiver)))
      
(defn pop-tuple-for! [router receiver]
  "Removes the next tuple to be sent to receiver from its queue. If the queue had been full and is now empty, returns the sender to be notified."
  (let [to-notify (partial senders-to-notify-of receiver)
        to-notify-before (to-notify router)
        router (pop-tuple-for router receiver)]
    [router (-> (difference to-notify-before (to-notify router)) first)]))



; { receiver        { :qs-by-sender                 { sender q }
;                     :turn                         sender
;                     :senders-to-notify-when-empty #{sender} } 
;   :max-queue-size x }
(defn create-router [max-queue-size]
  { :max-queue-size max-queue-size })