(ns sneer.message-subs
  (:require sneer.contacts
            [sneer.async :refer [go-loop-trace]]
            [sneer.tuple-base-provider :refer [tuple-base-of]]
            [sneer.tuple.persistent-tuple-base :refer [store-sub]]
            [clojure.set :refer [difference]]
            [clojure.core.async :refer [chan <!]])
  (:import [sneer.commons Container]
           [sneer.admin SneerAdmin]))

(def handle :message-subs)

(defn from [^Container container]
  (.produce container handle))

(defn start! [^Container container]
  (let [admin         ^SneerAdmin (.produce container SneerAdmin)
        own-puk       (.. admin privateKey publicKey)
        tuple-base    (tuple-base-of admin)
        contacts      (sneer.contacts/from container)
        contacts-puks (sneer.contacts/tap contacts (chan 1 (map sneer.contacts/puks)))]

    (go-loop-trace [subscribed-puks #{}]
      (when-some [puks (<! contacts-puks)]
        (doseq [puk (difference puks subscribed-puks)]
          (store-sub tuple-base own-puk {"type" "message" "audience" own-puk "author" puk}))
        (recur puks)))))
