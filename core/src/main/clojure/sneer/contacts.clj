(ns sneer.contacts
  (:require
    [clojure.core.async :refer [chan <! >! alt! go close!]]
    [sneer.async :refer [go-trace state-machine tap-state peek-state! go-loop-trace wait-for! encode-nil sliding-chan close-with!]]
    [sneer.commons :refer [now nvl]]
    [sneer.flux :refer [tap-actions response request]]
    [sneer.keys :refer [from-hex]]
    [sneer.rx :refer [obs-tap]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.protocols :refer [store-tuple query-with-history]])
  (:import
    [sneer.commons Container]
    [sneer.flux Dispatcher]
    [sneer.admin SneerAdmin]
    [sneer.commons.exceptions FriendlyException]
    [java.util UUID]))

(def handle ::contacts)

(defn from [^Container container]
  (.produce container handle))

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

(defn- store-contact! [container nick contact-puk invite-code]
  (-store-tuple! container {"type"        "contact"
                            "payload"     nick
                            "party"       contact-puk
                            "invite-code" invite-code}))

(defn- -puk [id state]
  (get-in state [:id->contact id :puk]))

(defn- -invite-code [id state]
  (get-in state [:id->contact id :invite-code]))

(defn- -nickname [id state]
  (get-in state [:id->contact id :nick]))

(defn- -contact-id [puk state]
  (get-in state [:puk->id puk]))

(defn contact-list [state]
  (-> state :id->contact vals))

(defn tap [contacts & [ch]]
 (-> contacts :machine (tap-state ch)))

(defn tap-id [contacts id lease]
  (let [xcontact (comp (map (fn [state] (get-in state [:id->contact id])))
                       (filter some?))
        result (sliding-chan 1 xcontact)]
    (close-with! lease result)
    (tap contacts result)
    result))

(defn id->puk [contacts id]
  (go
    (let [lease (chan)
          contact (<! (tap-id contacts id lease))
          puk (contact :puk)]
      (close! lease)
      puk)))

#_{:id->contact {42 {:id 42
                     :nick "Neide"
                     :puk NeidePuk
                     :invite-code "ea4e35a3ea54e3"
                     :timestamp long}}
   :nick->id        {"Neide" 42}
   :puk->id         {NeidePuk "Neide"}
   :invite-code->id {"ea4e35a3ea54e3" 42}}
(defn- handle-contact [state own-puk tuple]
  (if-not (= (tuple "author") own-puk)
    state
    (let [{new-nick "payload" puk "party" invite-code "invite-code" timestamp "timestamp"} tuple
          id (or (-contact-id puk state) (tuple "id"))
          old-nick (-nickname id state)
          contact {:id id
                   :nick new-nick
                   :puk puk
                   :invite-code invite-code
                   :timestamp timestamp}]
      (-> state
          (assoc-in  [:id->contact id] contact)
          (update-in [:nick->id] dissoc old-nick)
          (assoc-in  [:nick->id new-nick] id)
          (assoc-in  [:puk->id puk] id)
          (assoc-in  [:invite-code->id invite-code] id)))))

(defn- handle-push [state tuple]
;; {"type" "push" "audience" contact-puk "invite-code" invite-code-received}
  (let [invite-code (tuple "invite-code")
        contact-puk (tuple "author")
        inviter-puk (tuple "audience")]
    (if-some [id (get-in state [:invite-code->id invite-code])]
      (-> state
          (update-in [:id->contact id] assoc :puk contact-puk :timestamp (tuple "timestamp"))
          (update-in [:id->contact id] dissoc :invite-code)
          (assoc-in  [:puk->id contact-puk] id)
          (assoc-in  [:inviter-puk->id inviter-puk] id)
          (update-in [:invite-code->id] dissoc invite-code))
      state)))

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

(defn wait-for-store-contact! [container nick contact-puk invite-code contacts-states]
  (go-trace
    (let [tuple (<! (store-contact! container nick contact-puk invite-code))
          id (tuple "id")
          state (<! (wait-for! contacts-states #(up-to-date? % id)))]
      {:state state
       :id    id})))

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
                            result (<! (wait-for-store-contact! container nick nil invite-code states))]
                        (>! (response action) (result :id))
                        (result :state))))

                  "accept-invite"
                  (let [{:strs [nick puk-hex invite-code-received]} action]
                    (if-let [problem (problem-with-nick state nick)]
                        (do
                          (>! (response action) (FriendlyException. (str "Nickname " problem)))
                          state)
                        (let [contact-puk (from-hex puk-hex)
                              _ (<! (-store-tuple! container {"type" "push" "audience" contact-puk "invite-code" invite-code-received}))
                              result (<! (wait-for-store-contact! container nick contact-puk nil states))]
                          (>! (response action) (result :id))
                          (result :state))))

                  "find-convo"
                  (let [{:strs [inviter-puk]} action]
                    (if-let [inviter-puk->id (get-in state [:puk->id (from-hex inviter-puk)])]
                      (>! (response action) inviter-puk->id)))

                  "problem-with-new-nickname"
                  (let [{:strs [nick]} action]
                    (>! (response action) (encode-nil (problem-with-nick state nick)))
                    state)

                  "set-nickname"
                  (let [{:strs [contact-id new-nick]} action]
                    (if-let [problem (problem-with-nick state new-nick)]
                      (do
                        ; TODO: Emit in "toast" observable: (str "Nickname " problem)
                        state)
                      (let [contact-puk (-puk contact-id state)]
                        (if contact-puk
                          (let [result (<! (wait-for-store-contact! container new-nick contact-puk nil states))]
                            (result :state))
                          state))))  ;TODO: Change nickname even without puk, using id as "entity-id" in tuple.

                  state)))))))))

(defn problem-with-new-nickname [contacts nick]
  (.request (contacts :dispatcher) (request "problem-with-new-nickname" "nick" nick)))

(defn invite-code [contacts id]
  (obs-tap (contacts :machine) "invite-code tap" (map #(encode-nil (-invite-code id %)))))

(defn nickname [contacts id]
  (obs-tap (contacts :machine) "nickname tap" (map #(-nickname id %))))

(defn new-contact [contacts nick]
  (.request (contacts :dispatcher) (request "new-contact" "nick" nick)))

(defn accept-invite [contacts nick puk-hex invite-code-received]
  (.request (contacts :dispatcher) (request "accept-invite" "nick" nick "puk-hex" puk-hex "invite-code-received" invite-code-received)))

(defn find-convo [contacts inviter-puk]
  (.request (contacts :dispatcher) (request "find-convo" "inviter-puk" inviter-puk)))

(defn start! [container]
  (let [machine (tuple-machine! container)]
    (handle-actions! container machine)
    {:machine    machine
     :dispatcher (.produce container Dispatcher)}))
