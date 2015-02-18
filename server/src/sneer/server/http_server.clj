(ns sneer.server.http-server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [clojure.core.async :as async :refer [go <! >! >!! alt!]]
            [clojure.core.match :refer [match]]
            [sneer.async :refer [go-while-let go-trace sliding-chan]]
            [sneer.keys :as keys]
            [sneer.server.prevalence :as p]
            [sneer.server.gcm :as gcm]))

(defn- gcm-notification-queue []
  {:puks-to-notify #{}
   :puk->gcm-id {}})

(defn- gcm-enqueue [gcm-q puk]
  (update-in gcm-q [:puks-to-notify] conj puk))

(defn- gcm-dequeue [gcm-q puk]
  (update-in gcm-q [:puks-to-notify] disj puk))

(defn- gcm-assoc [gcm-q puk gcm-id]
  (assoc-in gcm-q [:puk->gcm-id puk] gcm-id))

(defn- gcm-round [{:keys [puks-to-notify puk->gcm-id]}]
  (select-keys puk->gcm-id puks-to-notify))

(defn- async-gcm-notify [gcm-id]
  (let [result (async/chan)]
    (gcm/send-to gcm-id
                 (fn [response] (>!! result response)))
    result))

(defn- timeout-secs [secs]
  (when-not (zero? secs)
    (println "GCM: WAITING" secs "SECONDS"))
  (async/timeout (* 1000 secs)))

(defn- retry-after [response]
  (if-let [retry-after-secs (-> response :headers :retry-after)]
    (Integer/valueOf retry-after-secs)
    0))

(defn- start-gcm-notification-rounds [gcm-qs puks-notified async-gcm-notify-fn]
  (go-while-let [gcm-q (<! gcm-qs)]
    (let [round (gcm-round gcm-q)]
      (println (str "GCM ROUND STARTED: " round))
      (loop [round round
             wait-after-round 0]
        (if (empty? round)
          (<! (timeout-secs wait-after-round))
          (let [[puk gcm-id] (first round)
                response (<! (async-gcm-notify-fn gcm-id))
                status (:status response)]
            (println "GCM RESPONSE:" response)
            (when (= 200 status)
              (>! puks-notified puk))
            (recur (rest round)
                   (max wait-after-round (retry-after response)))))))))

(defn- handle-gcm-event [gcm-q event]
  (try
    (match event
      [:assoc [puk gcm-id]] (gcm-assoc gcm-q puk gcm-id)
      [:enqueue puk] (gcm-enqueue gcm-q puk)
      [:dequeue puk] (gcm-dequeue gcm-q puk))
    (catch Exception e
      (if-not (-> gcm-q :puks-to-notify set?)   ; It was actually a list once in the prevalent state possibly due to some old bug using disj on nil.
        (do
          (println "MAKING :puks-to-notify A SET AGAIN")
          (assoc gcm-q :puks-to-notify #{}))
        (do
          (.printStackTrace e)
          (println (str "GCM QUEUE: " gcm-q "EVENT:" event))
          gcm-q)))))

(defn- gcm-queue-prevayler! [prevalence-file]
  (let [prevayler-jr! (partial p/prevayler-jr! handle-gcm-event (gcm-notification-queue))]
    (if (some? prevalence-file)
      (prevayler-jr! prevalence-file)
      (prevayler-jr!))))

(defn- start-gcm-queue-coordinator [prevalence-file gcm-qs puks-notified puk->gcm-id-in puks-in]

  (let [gcm-q (gcm-queue-prevayler! prevalence-file)
        -handle! #(p/handle! gcm-q [%1 %2])]

    (go-trace
      (loop []
        (>! gcm-qs @gcm-q)
        (alt!
          puks-notified  ([puk]
                           (-handle! :dequeue puk)
                           (recur))

          puks-in        ([puk]
                          (when puk
                            (-handle! :enqueue puk)
                            (recur)))

          puk->gcm-id-in ([puk->gcm-id]
                          (when puk->gcm-id
                            (-handle! :assoc puk->gcm-id)
                            (recur))))))))

(defn- start-gcm-notifier [prevalence-file puk->gcm-id-in puks-in async-gcm-notify-fn]
  (let [gcm-qs (sliding-chan)
        puks-notified (async/chan 10)]
    (start-gcm-notification-rounds gcm-qs puks-notified async-gcm-notify-fn)
    (start-gcm-queue-coordinator prevalence-file gcm-qs puks-notified puk->gcm-id-in puks-in)))

(defn- gcm-register [puk->gcm-id req]
  (let [params (:query-params req)
        hex-puk (get params "puk")
        id (get params "id")]
    (assert (not (or (empty? hex-puk) (empty? id))))
    (let [puk (keys/from-hex hex-puk)]
      (go (>! puk->gcm-id [puk id])))
    (str "id for " hex-puk " set to " id)))

(defn- log-calls [function]
  (fn [arg]
    (println arg)
    (function arg)))

(defn start [prevalence-file port puks-in & [async-gcm-notify-fn]]

  (let [puk->gcm-id-out (async/chan)
        async-gcm-notify-fn (or async-gcm-notify-fn async-gcm-notify)]

    (defroutes app
      (GET "/gcm/register" req (gcm-register puk->gcm-id-out req))
      (route/not-found "<h1>page not found</h1>"))

    (let [server (http/run-server (-> app log-calls params/wrap-params) {:port port})]
      (go
        (<! (start-gcm-notifier prevalence-file puk->gcm-id-out puks-in async-gcm-notify-fn))
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
