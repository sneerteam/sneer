(ns sneer.server.http-server-test
  (:require [sneer.server.http-server :as subject]
            [sneer.keys :as keys]
            [sneer.test-util :refer [>!!? <!!?]]
            [clojure.core.async :as async]
            [org.httpkit.client :as http]
            [midje.sweet :refer :all]))

; (do (require 'midje.repl) (midje.repl/autotest))

#_(let [to-google (async/chan)
      from-google (async/chan)
      gcm-notify (fn [id] (do (>!!? to-google id)
                              from-google))
      puks-to-notify (async/chan)
      no-prevalence-file nil]

  (subject/start no-prevalence-file 4242 puks-to-notify gcm-notify)

  (try
    (tabular "Pending notification is sent after registration"

      (let [puk (keys/->puk name?)]

        (>!!? puks-to-notify puk)

        (let [uri (str "http://localhost:4242/gcm/register?id=" gcm-id? "&puk=" (.toHex puk))
              response @(http/get uri)]
          (:status response) => 200)

        (<!!? to-google) => gcm-id?
        (>!!? from-google {:status 200}))



      name? gcm-id?
      "neide" "101010"
      "maicon"  "202020")

    (finally
      (async/close! puks-to-notify))))
