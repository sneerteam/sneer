(ns sneer.server.http-server-test
  (:require [sneer.server.http-server :as subject]
            [sneer.keys :as keys]
            [sneer.test-util :refer [>!!? <!!?]]
            [clojure.core.async :as async]
            [org.httpkit.client :as http]
            [midje.sweet :refer :all]))

(tabular "Pending notification is sent after registration"
  (let [puks-to-notify (async/chan)]
    (try
      (let [to-google (async/chan)
            from-google (async/chan)
            gcm-notify (fn [id] (do (>!!? to-google id)
                                    from-google))
            puk (keys/->puk name?)]

        (subject/start 4242 puks-to-notify gcm-notify)

        (>!!? puks-to-notify puk)

        (let [uri (str "http://localhost:4242/gcm/register?id=" gcm-id? "&puk=" (.toHex puk))
              response @(http/get uri)]
          (:status response) => 200)

        (<!!? to-google) => gcm-id?
        (>!!? from-google {:status 200}))

      (finally
        (async/close! puks-to-notify))))

  name?     gcm-id?
  "neide"   "101010"
  "maicon"  "202020")
