(ns sneer.server.http-server-test
  (:require [sneer.server.http-server :as server]
            [sneer.keys :as keys]
            [sneer.test-util :refer [>!!? <!!?]]
            [clojure.core.async :as async]
            [org.httpkit.client :as http]
            [midje.sweet :refer :all]))

(def puk (keys/->puk "neide"))

(def gcm-id "101010")

(fact "Pending notification is sent after registration"
  (let [puks-out (async/chan)]
    (try

      (let [notified     (async/chan 1)
            gcm-response (async/chan)
            gcm-notify   (fn [id] (do (>!!? notified id)
                                     gcm-response))]

        (server/start 4242 puks-out gcm-notify)

        (>!!? puks-out puk)

        (let [uri (str "http://localhost:4242/gcm/register?id=" gcm-id "&puk=" (.toHex puk))
              response @(http/get uri)]
          (:status response) => 200)

        (<!!? notified) => gcm-id
        (>!!? gcm-response {:status 200}))

      (finally
        (async/close! puks-out)))))
