(ns sneer.contacts
  (:require
    [clojure.core.async :refer [chan <! >! alt!]]
    [sneer.async :refer [state-machine tap-state peek-state! go-loop-trace wait-for! encode-nil]]
    [sneer.commons :refer [now nvl]]
    [sneer.flux :refer [tap-actions response request]]
    [sneer.keys :refer [from-hex]]
    [sneer.rx :refer [obs-tap]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.protocols :refer [store-tuple query-with-history]])
  (:import
    [sneer.flux Dispatcher]
    [sneer.admin SneerAdmin]
    [sneer.commons.exceptions FriendlyException]
    [java.util UUID]))

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

(defn- store-contact! [container new-contact-nick contact-puk invite-code]
  (-store-tuple! container {"type"        "contact"
                            "payload"     new-contact-nick
                            "party"       contact-puk
                            "invite-code" invite-code}))

#_{:nick->id        {"Neide" 42}
   :id->nick        {42 "Neide"}
   :id->invite-code {42 "ea4e35a3ea54e3"}
   :invite-code->id {"ea4e35a3ea54e3" 42}
   :puk->nick       {NeidePuk "Neide"}}
(defn- handle-contact [state own-puk tuple]
  (if-not (= (tuple "author") own-puk)
    state
    (let [new-nick (tuple "payload")
          puk (tuple "party")
          old-nick (get-in state [:puk->nick puk])
          id (get-in state [:nick->id old-nick])
          id (or id (tuple "id"))
          invite-code (tuple "invite-code")]
      (-> state
          (update-in [:nick->id] dissoc old-nick)
          (assoc-in  [:nick->id new-nick] id)
          (assoc-in  [:id->nick id] new-nick)
          (assoc-in  [:puk->nick puk] new-nick)
          (assoc-in  [:id->invite-code id] invite-code)
          (assoc-in  [:invite-code->id invite-code] id)))))

(defn- handle-push [state tuple]
  ; (-store-tuple! container {"type" "push" "audience" contact-puk "invite-code" invite-code-received})
  (let [invite-code (tuple "invite-code")
        id   (get-in state [:invite-code->id invite-code])
        nick (get-in state [:id->nick] id)]
    (-> state
      (assoc-in  [:puk->nick (tuple "author")] nick)
      (update-in [:id->invite-code] dissoc id)
      (update-in [:invite-code->id] dissoc invite-code))))

(defn- handle-tuple [own-puk state tuple]
  (let [state (case (tuple "type")
                "contact" (handle-contact state own-puk tuple)
                "push"    (handle-push    state tuple)
                state)]
    (assoc state :last-id (tuple "id"))))

(defn- tuple-base [container]
  (tuple-base-of (.produce container SneerAdmin)))

(defn- tuple-machine! [container]
  (let [old-tuples (chan 1)
        new-tuples (chan 1)
        lease (.produce container :lease)
        own-puk (own-puk container)]
    (query-with-history (tuple-base container) {#_after-id #_starting-id} old-tuples new-tuples lease)
    (state-machine (partial handle-tuple own-puk) {:last-id 0} old-tuples new-tuples)))

(defn- problem-with-nick [state nick]
  (cond
    (.isEmpty nick) "cannot be empty"
    (get-in state [:nick->id nick]) "already used"
    :else nil))

(defn- up-to-date? [state id]
  (>= (state :last-id) id))

(defn- handle-actions! [container tuple-machine]
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
                      (let [invite-code (-> (UUID/randomUUID) .toString (.replaceAll "-" ""))
                            tuple (<! (store-contact! container nick nil invite-code))
                            id (tuple "id")
                            state' (<! (wait-for! states #(up-to-date? % id)))]
                        (>! (response action) id)
                        state')))

                  "accept-invite"
                  (let [{:strs [nick puk-hex invite-code-received]} action]
                    (if-let [problem (problem-with-nick state nick)]
                      (do
                        (>! (response action) (FriendlyException. (str "Nickname " problem)))
                        state)
                      (let [contact-puk (from-hex puk-hex)
                            _ (<! (-store-tuple! container {"type" "push" "audience" contact-puk "invite-code" invite-code-received}))
                            tuple (<! (store-contact! container nick contact-puk nil))
                            id (tuple "id")
                            state' (<! (wait-for! states #(up-to-date? % id)))]
                        (>! (response action) id)
                        state')))

                  "problem-with-new-nickname"
                  (let [{:strs [nick]} action]
                    (>! (response action) (encode-nil (problem-with-nick state nick)))
                    state)

;                  "set-nickname"
                  #_(let [{:strs [nick]} action]
                    (if-let [problem (problem-with-nick state nick)]
                      (do
                        ; Emit in "toast" observable: (>! (response action) (FriendlyException. (str "Nickname " problem)))
                        state)
                      (let [tuple (<! (store-contact! container nick nil))]
                        (>! (response action) (tuple "id"))
                        (<! (wait-for! states #(up-to-date? % (tuple "id")))))))


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

(defn problem-with-new-nickname [contacts nick]
  (.request (contacts :dispatcher) (request "problem-with-new-nickname" "nick" nick)))

(defn- -invite-code [id state]
  (get-in state [:id->invite-code id]))

(defn invite-code [contacts id]
  (obs-tap (contacts :machine) "invite-code tap" (map #(encode-nil (-invite-code id %)))))

(defn- -nickname [id state]
  (get-in state [:id->nick id]))

(defn nickname [contacts id]
  (obs-tap (contacts :machine) "nickname tap" (map #(-nickname id %))))

(defn new-contact [contacts nick]
  (.request (contacts :dispatcher) (request "new-contact" "nick" nick)))

(defn accept-invite [contacts nick puk-hex invite-code-received]
  (.request (contacts :dispatcher) (request "accept-invite" "nick" nick "puk-hex" puk-hex "invite-code-received" invite-code-received)))

(defn start! [container]
  (let [machine (tuple-machine! container)]
    (handle-actions! container machine)
    {:machine    machine
     :dispatcher (.produce container Dispatcher)}))
