(ns sneer.server.router
  (:require
    [sneer.commons :refer [empty-queue]]
    [clojure.set :refer [difference]]))

(defn- dropping-conj [xs x max-size]
  (let [full? (>= (count xs) max-size)]
    (if full? xs (conj xs x))))

(defn- dropping-enqueue [q element max-size]
  (dropping-conj (or q empty-queue) element max-size))

(defn- next-wrap [coll element]
  (let [count (count coll)]
    (if (zero? count)
      nil
      (let [vec (vec coll)
            index (.indexOf vec element)]
        (get vec (mod (inc index) count))))))

(defn- peek-tuple-for [receiver-q]
  (let [{:keys [qs-by-sender turn]} receiver-q
        tuple (some-> turn qs-by-sender peek)]
    (when tuple {:send tuple})))

(defn- peek-cts-for [receiver-q]
  (when-some [peer (-> receiver-q :receivers-cts peek)]
    {:cts peer}))

(defn- peek-for [receiver-q]
  (or
    (peek-cts-for receiver-q)
    (peek-tuple-for receiver-q)))

(defn- mark-full [receiver-q sender]
  (update-in receiver-q [:senders-to-notify-when-cts] (fnil conj #{}) sender))

(defn- sender-queue-count [receiver-q sender]
  (-> receiver-q (get-in [:qs-by-sender sender]) count))

(defn- sender-queue-full? [receiver-q sender max-queue-size]
  "Returns whether the receiver/sender send queue is full."
  (>= (sender-queue-count receiver-q sender) max-queue-size))

(defn- enqueue-for [receiver-q queue-size sender tuple]
  (let [turn (:turn receiver-q)
        receiver-q (update-in receiver-q [:qs-by-sender sender] dropping-enqueue tuple queue-size)
        receiver-q (if turn
                     receiver-q
                     (assoc receiver-q :turn sender))]

    (if (sender-queue-full? receiver-q sender queue-size)
      (mark-full receiver-q sender)
      receiver-q)))

(defn- pop-tuple-packet [receiver-q]
    (let [turn (:turn receiver-q)
        receiver-q (update-in receiver-q [:qs-by-sender turn] pop)
        sender-q-empty? (nil? (peek-tuple-for receiver-q))
        senders (-> receiver-q :qs-by-sender keys)
        receiver-q (assoc receiver-q :turn (next-wrap senders turn))
        sender-to-notify (get-in receiver-q [:senders-to-notify-when-cts turn])
        receiver-q (if sender-q-empty?
                     (-> receiver-q
                       (update-in [:qs-by-sender] dissoc turn)
                       (update-in [:senders-to-notify-when-cts] disj turn)) ; If there was only one sender, :turn will point to it (removed sender) but that's ok because receiver-q will be empty and will be removed from qs. 
                     receiver-q)]
      [receiver-q (when sender-q-empty? sender-to-notify)]))

(defn- pop-cts-packet [receiver-q]
  (update-in receiver-q [:receivers-cts] pop))

(defn- pop-packet [receiver-q]
  (if (peek-cts-for receiver-q)
    (assert false)
    (pop-tuple-packet receiver-q)))

(defn- enqueue-cts [router sender receiver]
  (update-in router [sender :receivers-cts] (fnil conj empty-queue) receiver))

(defn enqueue! [router sender receiver tuple]
  "Adds tuple to its receiver/sender send queue. Pre-requisite: the queue is not full."
  (update-in router [receiver] enqueue-for (router :max-queue-size) sender tuple))

(defn peek-packet-for [router receiver]
  "Returns the next tuple to be sent to receiver."
  (peek-for (router receiver)))

(defn pop-packet-for [router receiver]
  "Removes the next tuple to be sent to receiver from its queue."
  (let [[receiver-q sender-cts] (-> router (get receiver) pop-packet)
        router (if sender-cts
                 (enqueue-cts router sender-cts receiver)
                 router)
        empty? (-> receiver-q peek-for nil?)]
    (if empty?
      (dissoc router receiver)
      (assoc router receiver receiver-q))))

(defn queue-full? [router sender receiver]
  "Returns whether the receiver/sender send queue is full."
  (sender-queue-full? (router receiver) sender (router :max-queue-size)))


; { :max-queue-size x
;   receiver        { :qs-by-sender                { sender q }
;                     :turn                        sender
;                     :senders-to-notify-when-cts  #{sender}
;                     :receivers-cts               q } }
;
(defn create-router [max-queue-size]
  { :max-queue-size max-queue-size })