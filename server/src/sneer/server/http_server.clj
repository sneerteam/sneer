(ns sneer.server.http-server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [clojure.core.async :as async :refer [<! >! >!! go-loop alt!]]
            [sneer.async :refer [go-while-let sliding-chan]]
            [sneer.keys :as keys]
            [sneer.server.gcm :as gcm]))

(defn async-gcm-notify [gcm-id]
  (let [result (async/chan)]
    (gcm/send-to gcm-id
                 (fn [response] (>!! result response)))
    result))

(defn- start-gcm-notification-rounds [gcm-ids gcm-ids-notified]
  (go-while-let [round (<! gcm-ids)]
    (loop [round round]
      (when-not (empty? round)
        (let [gcm-id (first round)
              response (<! (async-gcm-notify gcm-id))
              status (:status response)]
          (println "GCM: " response)
          (if (= 200 status)
            (>! gcm-ids-notified gcm-id)
            (when-some [retry-after-secs (-> response :headers :retry-after)]
              (println "GCM: WAITING" retry-after-secs "SECONDS")
              (<! (async/timeout (* 1000 (Integer/valueOf retry-after-secs))))))
          (recur (rest round)))))))

(defn- start-gcm-notifier [puk->gcm-id-in puks-in]

  (let [gcm-ids-out (sliding-chan)
        gcm-ids-notified (async/chan)]

    (start-gcm-notification-rounds gcm-ids-out gcm-ids-notified)

    (go-loop [puk->gcm-id {}
              gcm-id->puk {}
              puks-to-notify #{}
              ids-to-notify #{}
              previous-ids-to-notify ids-to-notify]

      (when-not (identical? previous-ids-to-notify ids-to-notify)
        (>! gcm-ids-out ids-to-notify))

      (alt!
        puks-in
        ([puk]
         (when (some? puk)
           (recur puk->gcm-id
                  gcm-id->puk
                  (conj puks-to-notify puk)
                  (if-some [gcm-id (get puk->gcm-id puk)]
                    (conj ids-to-notify gcm-id)
                    ids-to-notify)
                  ids-to-notify)))

        puk->gcm-id-in
        ([[puk gcm-id]]
         (recur (assoc puk->gcm-id-in puk gcm-id)
                (assoc gcm-id->puk gcm-id puk)
                puks-to-notify
                (if (contains? puks-to-notify puk)
                  (conj ids-to-notify gcm-id)
                  ids-to-notify)
                ids-to-notify))

        gcm-ids-notified
        ([gcm-id]
         (recur puk->gcm-id
                gcm-id->puk
                (disj puks-to-notify (get gcm-id->puk gcm-id))
                (disj ids-to-notify gcm-id)
                ids-to-notify))))))

(defn- gcm-register [puk->gcm-id req]
  (let [params (:query-params req)
        puk (get params "puk")
        id (get params "id")]
    (assert (not (or (empty? puk) (empty? id))))
    (async/go
      (>! puk->gcm-id [(keys/->puk puk) id]))
    (str "id for " puk " set to " id)))

(defn start [port puks-in]

  (let [puk->gcm-id-out (async/chan)]

    (defroutes app
      (GET "/gcm/register" req (gcm-register puk->gcm-id-out req))
      (route/not-found "<h1>page not found</h1>"))

    (let [server (http/run-server (-> app params/wrap-params) {:port port})]
      (async/go
        (<! (start-gcm-notifier puk->gcm-id-out puks-in))
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
