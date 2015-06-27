(ns sneer.contacts
  (:require
    [clojure.core.async :refer [chan <! >! alt!]]
    [sneer.async :refer [state-machine tap-state peek-state! go-loop-trace wait-for!]]
    [sneer.commons :refer [now nvl]]
    [sneer.flux :refer [tap-actions response request request!!]]
    [sneer.keys :refer [from-hex]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.protocols :refer [store-tuple query-with-history]])
  (:import
    [sneer.flux Dispatcher]
    [sneer.admin SneerAdmin]
    [sneer.commons.exceptions FriendlyException]))

(def handle ::contacts)

(defn- puk [^SneerAdmin admin]
  (.. admin privateKey publicKey))

(defn- own-puk [container]
  (puk (.produce container SneerAdmin)))

(defn- -store-tuple! [container tuple]
  (let [admin (.produce container SneerAdmin)
        own-puk (puk admin)
        tuple-base (tuple-base-of admin)
        defaults {"timestamp" (now)
                  "audience"  own-puk
                  "author"    own-puk}]
      (store-tuple tuple-base (merge defaults tuple))))

(defn- store-contact! [container new-contact-nick contact-puk]
  (-store-tuple! container {"type"    "contact"
                            "payload" new-contact-nick
                            "party"   contact-puk}))

(defn- contact-puk [tuple]
  (tuple "party"))

#_{:nick->id  {"Neide" 42}
   :puk->nick {NeidePuk "Neide"}}
(defn- handle-contact [own-puk state tuple]
  (if-not (= (tuple "author") own-puk)
    state
    (let [new-nick (tuple "payload")
          puk (contact-puk tuple)
          old-nick (get-in state [:puk->nick puk])
          id (get-in state [:nick->id old-nick])
          id (or id (tuple "id"))]
      (-> state
          (update-in [:nick->id] dissoc old-nick)
          (assoc-in  [:nick->id new-nick] id)
          (assoc-in  [:puk->nick puk] new-nick)))))


(defn- handle-tuple [own-puk state tuple]
  (->
    (handle-contact own-puk state tuple)
    (assoc :last-id (tuple "id"))))

(defn- tuple-base [container]
  (tuple-base-of (.produce container SneerAdmin)))

(defn problem-with-new-nickname [contacts nick]
  (.request (contacts :dispatcher) (request "problem-with-new-nickname" "nick" nick)))

(defn tuple-machine! [container]
  (let [old-tuples (chan 1)
        new-tuples (chan 1)
        lease (.produce container :lease)
        own-puk (own-puk container)]
    (query-with-history (tuple-base container) {"type" "contact" #_after-id #_starting-id} old-tuples new-tuples lease)
    (state-machine (partial handle-tuple own-puk) {:last-id 0} old-tuples new-tuples)))

(defn- problem-with-nick [state nick]
  (cond
    (.isEmpty nick) "cannot be empty"
    (get-in state [:nick->id nick]) "already used"
    :else nil))

(defn up-to-date? [state id]
  (>= (state :last-id) id))

(defn handle-actions! [container tuple-machine]
  (let [states (tap-state tuple-machine)
        actions (chan 1)]
    (tap-actions (.produce container Dispatcher) actions)

    (go-loop-trace [state (<! states)]
      (when state
        (recur
          (alt!
            states
            ([new-state] new-state)

            actions
            ([action]
              (when action
                (case (action :type)

                  "new-contact"
                  (let [{:strs [nick]} action]
                    (if-let [problem (problem-with-nick state nick)]
                      (do
                        (>! (response action) (FriendlyException. (str "Nickname " problem)))
                        state)
                      (let [tuple (<! (store-contact! container nick nil))]
                        (>! (response action) (tuple "id"))
                        (<! (wait-for! states #(up-to-date? % (tuple "id")))))))

                  "problem-with-new-nickname"
                  (let [{:strs [nick]} action]
                    (>! (response action) (nvl (problem-with-nick state nick) :nil))
                    state)

                  ;    "set-nickname"
                  #_(let [{:strs [convo-id new-nick]} action]
                                    (println "set-nickname" convo-id new-nick))

                  ;    "accept-invite"
                  #_(let [{:strs [new-contact-nick contact-puk invite-code-received]} action
                                        contact-puk (from-hex contact-puk)
                                        contact-tuple (<! (store-contact! container new-contact-nick contact-puk))]
                                    (-store-tuple! container {"type"        "push"
                                                              "audience"    contact-puk
                                                              "invite-code" invite-code-received})

                                    (>! (response a) (contact-tuple "id")))

                  (println "CONTACTS - UNKNOWN ACTION: " action))))))))))

(defn start! [container]
  (let [machine (tuple-machine! container)]
    (handle-actions! container machine)
    {:tuple-machine machine
     :dispatcher    (.produce container Dispatcher)}))
