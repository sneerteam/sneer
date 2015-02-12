(ns sneer.server.http-server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [clojure.core.async :as async :refer [<! >! >!! go-loop alts!]]
            [clojure.core.match :refer [match]]
            [sneer.async :refer [go-while-let go-loop-trace sliding-chan]]
            [sneer.keys :as keys]
            [sneer.server.gcm :as gcm]))

(defn- async-gcm-notify [gcm-id]
  (let [result (async/chan)]
    (gcm/send-to gcm-id
                 (fn [response] (>!! result response)))
    result))

(defn- wait [secs-string]
  (println "GCM: WAITING" secs-string "SECONDS")
  (async/timeout (* 1000 (Integer/valueOf secs-string))))

(defn- start-gcm-notification-rounds [rounds-to-notify puks-notified async-gcm-notify-fn]
  (go-while-let [round-fn (<! rounds-to-notify)]
    (let [round (-> (round-fn) seq)]
      (println "GCM ROUND STARTED:" round)
      (loop [round round]
        (when-not (empty? round)
          (let [[puk gcm-id] (first round)
                response (<! (async-gcm-notify-fn gcm-id))
                status (:status response)]
            (println "GCM RESPONSE:" response)
            (if (= 200 status)
              (>! puks-notified puk)
              (when-some [retry-after-secs (-> response :headers :retry-after)]
                (<! (wait retry-after-secs))))
            (recur (rest round))))))))

(defn- contains-any [keys coll]
  (some #(contains? coll %) keys))

(defn- start-gcm-notifier [puk->gcm-id-in puks-in async-gcm-notify-fn]

  (let [rounds-to-notify (async/chan)
        puks-notified (async/chan 10)
        input-channels [puks-in, puk->gcm-id-in, puks-notified]]

    (start-gcm-notification-rounds rounds-to-notify puks-notified async-gcm-notify-fn)

    (go-loop-trace [puk->gcm-id {}
                    puks-to-notify #{}]

      (let [[val ch] (alts! (if (contains-any puks-to-notify puk->gcm-id)
                              (conj input-channels [rounds-to-notify #(select-keys puk->gcm-id puks-to-notify)])
                              input-channels))]
        (match ch
          puks-in (when (some? val)
                    (recur puk->gcm-id
                           (conj puks-to-notify val)))

          puk->gcm-id-in (let [[puk gcm-id] val]
                           (recur (assoc puk->gcm-id puk gcm-id)
                                  puks-to-notify))

          puks-notified (recur puk->gcm-id
                               (disj puks-to-notify val))

          :else (recur puk->gcm-id puks-to-notify))))))

(defn- gcm-register [puk->gcm-id req]
  (let [params (:query-params req)
        hex-puk (get params "puk")
        id (get params "id")]
    (assert (not (or (empty? hex-puk) (empty? id))))
    (async/go
      (>! puk->gcm-id [(keys/from-hex hex-puk) id]))
    (str "id for " hex-puk " set to " id)))

(defn- log-calls [function]
  (fn [arg]
    (println arg)
    (function arg)))

(defn start [port puks-in & [async-gcm-notify-fn]]

  (let [puk->gcm-id-out (async/chan)
        async-gcm-notify-fn (or async-gcm-notify-fn async-gcm-notify)]

    (defroutes app
      (GET "/gcm/register" req (gcm-register puk->gcm-id-out req))
      (route/not-found "<h1>page not found</h1>"))

    (let [server (http/run-server (-> app log-calls params/wrap-params) {:port port})]
      (async/go
        (<! (start-gcm-notifier puk->gcm-id-out puks-in async-gcm-notify-fn))
        (async/close! puk->gcm-id-out)
        (server)))))

;; for REPL usage
;(def puks-in (async/chan))
;(def server (start 8080 puks-in))
;
;(defn stop []
;  (async/close! puks-in))
;
;(defn notify [puk]
;  (async/go
;    (async/>! puks-in puk)))
;
;(notify (keys/->puk "foo"))
;
;(stop)
