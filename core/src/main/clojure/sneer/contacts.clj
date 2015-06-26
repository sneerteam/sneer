(ns sneer.contacts
  (:require
    [clojure.core.async :refer [chan <!! >!]]
    [sneer.async :refer [state-machine tap-state]]
    [sneer.commons :refer [now]]
    [sneer.flux :refer [tap-actions response]]
    [sneer.keys :refer [from-hex]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.protocols :refer [store-tuple query-tuples]])
  (:import
    [sneer.flux Dispatcher]
    [sneer.admin SneerAdmin]))

(def handle ::contacts)

(defn- puk [^SneerAdmin admin]
  (.. admin privateKey publicKey))

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

(defn- handle-events! [container nicks action]
  (case (action :type)

    "new-contact"
    (let [{:strs [nick]} action]
      (<!! (store-contact! container nick nil))
      (conj nicks nick))

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

    (println "UNKNOWN ACTION: " action)))

#_(defn- contact-puk [tuple]
  (tuple "party"))

#_{:nick->id  {"Neide" 42}
   :puk->nick {NeidePuk "Neide"}}
#_(defn- handle-contact [own-puk state tuple]
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



(defn- tuple-base [container]
  (tuple-base-of (.produce container SneerAdmin)))

(defn tap-nicks! [machine tap-ch]
  (tap-state machine tap-ch))

(defn start! [container]
  (let [events (chan 1)
        _ (tap-actions (.produce container Dispatcher) events)
        lease (.produce container :lease)]
    (query-tuples (tuple-base container) #_{after-id starting-id} {} events lease)
    (state-machine #{} (partial handle-events! container) events)))
