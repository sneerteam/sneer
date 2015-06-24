(ns sneer.contacts
  (:require
    [clojure.core.async :refer [chan <! >!]]
    [sneer.async :refer [go-while-let]]
    [sneer.commons :refer [now]]
    [sneer.flux :refer [tap-actions response]]
    [sneer.tuple-base-provider :refer :all]
    [sneer.tuple.protocols :refer [store-tuple]])
  (:import
   [sneer.flux Dispatcher]
   [sneer.admin SneerAdmin]))

(def handle ::contacts)

(defn- puk [^SneerAdmin admin]
  (.. admin privateKey publicKey))

(defn- store-contact! [container newContactNick]
  (let [admin (.produce container SneerAdmin)
        own-puk (puk admin)
        tuple-base (tuple-base-of admin)]
    (store-tuple tuple-base {"type"      "contact"
                             "payload"   newContactNick
                             "timestamp" (now)
                             "audience"  own-puk
                             "author"    own-puk})))

(defn start! [container]
  (let [actions (chan 1)]
    (tap-actions (.produce container Dispatcher) actions)

    (go-while-let [a (<! actions)]

      (case (a :type)

        "set-nickname"
        (let [{:strs [convo-id new-nick]} a]
          (println "set-nickname" convo-id new-nick))

        "accept-invite"
        (let [{:strs [new-contact-nick contact-puk invite-code-received]} a]
          (println "accept-invite" new-contact-nick contact-puk invite-code-received)
          (>! (response a) 42))

        nil))))
