(ns sneer.server.router
  (:require
   [clojure.set :refer [difference]]))

(defprotocol Router
  (enqueue! [_ sender receiver tuple]
    "Adds tuple to its receiver/sender send queue if the queue isn't full. Returns whether the queue was able to accept tuple (queue was not full).")
  (peek-tuple-for [_ receiver]
    "Returns the next tuple to be sent to receiver.")
  (pop-tuple-for! [_ receiver]
    "Removes the next tuple to be sent to receiver from its queue. If the queue had been full and is now empty, returns the sender to be notified."))

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
