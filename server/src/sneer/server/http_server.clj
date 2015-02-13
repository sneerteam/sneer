(ns sneer.server.http-server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [clojure.core.async :as async :refer [<! >! >!! go-loop alts!]]
            [clojure.core.match :refer [match]]
            [sneer.async :refer [go-while-let go-loop-trace]]
            [sneer.keys :as keys]
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

(defn- wait [secs-string]
  (println "GCM: WAITING" secs-string "SECONDS")
  (async/timeout (* 1000 (Integer/valueOf secs-string))))

(defn- start-gcm-notification-rounds [gcm-qs puks-notified async-gcm-notify-fn]
  (go-while-let [gcm-q (<! gcm-qs)]
    (let [round (gcm-round gcm-q)]
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

(defn- start-gcm-notifier [puk->gcm-id-in puks-in async-gcm-notify-fn]

  (let [gcm-qs (async/chan)
        puks-notified (async/chan 10)
        input-channels [puks-in puks-notified puk->gcm-id-in]]

    (start-gcm-notification-rounds gcm-qs puks-notified async-gcm-notify-fn)

    (go-loop-trace [gcm-q (gcm-notification-queue)
                    previous-gcm-q gcm-q]

      (let [[val ch] (alts! (if (identical? previous-gcm-q gcm-q)
                              input-channels
                              (conj input-channels [gcm-qs gcm-q]))
                            ;; prioritized so puks-notified is handled before gcm-qs
                            :priority true)]
        (match ch
          puk->gcm-id-in (when-some [[puk gcm-id] val]
                           (recur (gcm-assoc gcm-q puk gcm-id) gcm-q))

          puks-in        (when-some [puk val]
                           (recur (gcm-enqueue gcm-q puk) gcm-q))

          puks-notified  (recur (gcm-dequeue gcm-q val) gcm-q)

          :else          (recur gcm-q gcm-q))))))

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
