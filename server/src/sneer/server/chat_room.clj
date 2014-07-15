(ns sneer.server.chat-room
  (:use amalloy.ring-buffer))


(defn now [] (System/currentTimeMillis))

(defn replies-to-msg [msg user-addresses last-messages]
  (let [timed-msg (assoc msg :timestamp (now))]
    (swap! last-messages conj timed-msg)
    (map #(vector % timed-msg) @user-addresses)))

(defn replies-to-refresh [sender-addr last-messages]
  (map #(vector sender-addr %) @last-messages))

(defn replies-to [user-addresses last-messages [sender-addr msg]]
  (swap! user-addresses conj sender-addr)
  (if (= (:type msg) :refresh)
    (replies-to-refresh sender-addr last-messages)
    (replies-to-msg msg user-addresses last-messages)))

(defn chat-room! []
  (let [user-addresses (atom #{})
        last-messages (atom (ring-buffer 10))]
    #(replies-to user-addresses last-messages %)))



;;; Test helper functions:

(defn assert-equals [actual expected]
  (if (= actual expected)
    :OK
    (throw (AssertionError. (str "Actual: " actual)))))


;;; Tests:

(defn run-test []
  (def clock (atom 0))
  (defn now [] (swap! clock inc))

  (def room (chat-room!))

  ;;; Incoming messages are sent back to the sender with a timestamp.
  (def replies (room ["user1" {:contents "Hi there"}]))
  (assert-equals (nth replies 0)
                 ["user1" {:timestamp 1 :contents "Hi there"}])

  ;;; Incoming messages are sent to all users. User addresses can be any value. Messages can be any map.
  (def replies (room [42 {:foo :bar}]))
  (assert-equals (nth replies 0)
                 ["user1" {:timestamp 2 :foo :bar}])
  (assert-equals (nth replies 1)
                 [42      {:timestamp 2 :foo :bar}])

  ;;; Refresh request gets last messages.
  (def replies (room ["user532" {:type :refresh}]))
  (assert-equals (nth replies 0)
                 ["user532" {:timestamp 1 :contents "Hi there"}])
  (assert-equals (nth replies 1)
                 ["user532" {:timestamp 2 :foo :bar}])

  ;;; Refresh request get only the last 10 messages.
  (dotimes [_ 100] (room [42 { :foo :bar }]))
  (def replies (room ["user532" {:type :refresh}]))
  (assert-equals (nth replies 0)
                 ["user532" {:timestamp  93 :foo :bar}])
  (assert-equals (nth replies 9)
                 ["user532" {:timestamp 102 :foo :bar}]))

;(run-test)
