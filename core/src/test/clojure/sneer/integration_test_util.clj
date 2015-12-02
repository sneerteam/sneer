(ns sneer.integration-test-util
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [close! chan <! >!]]
            [sneer.async :refer [go-while-let]]
            [sneer.tuple.persistent-tuple-base :refer [starting-id]]
            [sneer.rx-test-util :refer [emits <next]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.transmitter :as transmitter])
  (:import [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder Startup]
           [sneer.impl CoreLoader]
           [java.io Closeable]
           [sneer.tuple.protocols Database]
           (sneer.admin SneerAdmin)
           (java.util Random)
           (sneer.convos Convos)))

(defn admin->puk [admin]
  (.. admin privateKey publicKey))

(defn puk [container]
  (admin->puk (container SneerAdmin)))

(defn- start [puk tb in out other-puk]
  (transmitter/start puk tb in
                     (fn [follower-puk tuples-out & _]
                       (assert (= follower-puk other-puk))
                       (go-while-let [[tuple ack-ch] (<! tuples-out)]
                         (>! out tuple)
                         (>! ack-ch tuple)))))

(defn connect-admins! [admin-a admin-b]
  (let [->a (chan)
        ->b (chan)
        puk-a (admin->puk admin-a)
        puk-b (admin->puk admin-b)
        tb-a (tuple-base-of admin-a)
        tb-b (tuple-base-of admin-b)]

    (start puk-a tb-a ->a ->b puk-b)
    (start puk-b tb-b ->b ->a puk-a)))

(defn connect! [container-a container-b]
  (connect-admins! (container-a SneerAdmin) (container-b SneerAdmin)))

(defn- avoid-id-coincidences []
  (swap! starting-id #(+ 1000 %)))

(defn sneer! []
  (avoid-id-coincidences)
  (let [delegate (Container. (CoreLoader.))
        transient nil]
    (.inject delegate PersistenceFolder (reify PersistenceFolder (get [_] transient)))
    (.inject delegate Database (create-sqlite-db))
    (.produce delegate Startup)

    (reify
      IFn
      (invoke [_ component] (.produce delegate component))

      Closeable
      (close [_] (close! (.produce delegate :lease))))))

(defn restarted! [old-sneer]
  (let [delegate (Container. (CoreLoader.))]
    (.inject delegate PersistenceFolder (old-sneer PersistenceFolder))
    (.inject delegate Database (old-sneer Database))
    (.produce delegate Startup)

    (.close old-sneer)

    (reify
      IFn
      (invoke [_ component] (.produce delegate component))

      Closeable
      (close [_] (close! (.produce delegate :lease))))))

(defn- connect-all! [community new-member]
  (doseq [member community]
    (connect! new-member member))
  (conj community new-member))

(defn- step [community random invites]
  (let [inviter-index (.nextInt random (count community))
        _ ()
        invited-index (.nextInt random (count community))
        inviter (community inviter-index)
        invited (community invited-index)
        inviter-convo-id (<next (.startConvo (inviter Convos) (str "Nick " invited-index)))
        inviter-convo (<next (.getById (inviter Convos) inviter-convo-id))
        invite-code (.inviteCodePending inviter-convo)]
    (println "CODE" invite-code)
    (println "ID" (<next (.acceptInvite (invited Convos) (str "Nick " inviter-index) invite-code)))))

(defn randest []
  (let [community (vec (repeatedly 2 sneer!))
        random (Random.)
        invites (atom (list))]
    (reduce connect-all! [] community)
    (repeatedly 50 #(step community random invites))))