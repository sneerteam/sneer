(ns sneer.server.http-server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [clojure.core.async :as async :refer [go <! >! >!! go-loop alts!]]
            [clojure.core.match :refer [match]]
            [sneer.async :refer [go-while-let go-trace]]
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
      (println "GCM ROUND STARTED:" round)
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
  (match event
    [:assoc [puk gcm-id]] (gcm-assoc gcm-q puk gcm-id)
    [:enqueue puk] (gcm-enqueue gcm-q puk)
    [:dequeue puk] (gcm-dequeue gcm-q puk)))

(defn- gcm-queue-prevayler! [prevalence-file]
  (let [prevayler-jr! (partial p/prevayler-jr! handle-gcm-event (gcm-notification-queue))]
    (if (some? prevalence-file)
      (prevayler-jr! prevalence-file)
      (prevayler-jr!))))

(defn- start-gcm-queue-coordinator
  [prevalence-file gcm-qs puks-notified puk->gcm-id-in puks-in]

  (let [input-channels [puks-in puks-notified puk->gcm-id-in]
        gcm-q (gcm-queue-prevayler! prevalence-file)
        -handle! #(let [previous @gcm-q]
                   (p/handle! gcm-q [%1 %2])
                   previous)]
    (go-trace

      (loop [previous-gcm-q nil]
        (let [[val ch] (alts! (if (identical? previous-gcm-q @gcm-q)
                                input-channels
                                (conj input-channels [gcm-qs @gcm-q]))
                              ;; prioritized so puks-notified is handled before gcm-qs
                              :priority true)]
          (match ch
            puk->gcm-id-in (when (some? val)
                             (recur (-handle! :assoc val)))

            puks-in        (when (some? val)
                             (recur (-handle! :enqueue val)))

            puks-notified  (recur (-handle! :dequeue val))

            gcm-qs         (recur @gcm-q)))))))

(defn- start-gcm-notifier [prevalence-file puk->gcm-id-in puks-in async-gcm-notify-fn]
  (let [gcm-qs (async/chan)
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
