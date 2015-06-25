(ns sneer.contacts
  (:require
    [clojure.core.async :refer [chan <! >!]]
    [sneer.async :refer [go-while-let]]
    [sneer.commons :refer [now]]
    [sneer.flux :refer [tap-actions response]]
    [sneer.keys :refer [from-hex]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.protocols :refer [store-tuple]])
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

(defn start! [container]
  (let [actions (chan 1)]
    (tap-actions (.produce container Dispatcher) actions)

    (go-while-let [a (<! actions)]

      (case (a :type)

        "set-nickname"
        (let [{:strs [convo-id new-nick]} a]
          (println "set-nickname" convo-id new-nick))

        "accept-invite"
        (let [{:strs [new-contact-nick contact-puk invite-code-received]} a
              contact-puk (from-hex contact-puk)
              contact-tuple (<! (store-contact! container new-contact-nick contact-puk))]
          (-store-tuple! container {"type"     "push"
                                    "audience" contact-puk
                                    "invite-code" invite-code-received})

          (>! (response a) (contact-tuple "id")))

        (println "UNKNOWN ACTION: " a)))))
