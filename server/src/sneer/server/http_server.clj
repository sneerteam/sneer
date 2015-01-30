(ns sneer.server.http-server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [clojure.core.async :as async :refer [<!]]
            [sneer.async :refer [IMMEDIATELY]]
            [sneer.keys :as keys]))

(defn async-gcm-notify [gcm-id]
  (let [result (async/chan)]
    (async/go
      (async/>! result {:headers {"Retry-After" "120"}}))
    result))

(defn- gcm-register [puk->gcm-id req]
  (let [params (:query-params req)
        puk (get params "puk")
        id (get params "id")]
    (assert (not (or (empty? puk) (empty? id))))
    (swap! puk->gcm-id assoc (keys/->puk puk) id)
    (str "id for " puk " set to " id)))

(defn rand-set-element [set]
  (let [n (count set)]
    (when-not (zero? n)
      (nth (seq set) (rand-int n)))))

(def NEVER (async/chan))

(defn- start-gcm-notifier [puk->gcm-id puks-in]
  (async/go-loop [puks #{}
                  send-timeout NEVER]
    (async/alt!
      puks-in
      ([puk]
        (when (some? puk)
          (if (contains? @puk->gcm-id puk)
            (let [puks' (conj puks puk)
                  send-timeout' (if (identical? send-timeout NEVER)
                                  IMMEDIATELY
                                  send-timeout)]
              (recur puks' send-timeout'))
            (recur puks send-timeout))))

      send-timeout
      ([_]
        (let [puk (rand-set-element puks)
              gcm-id (get @puk->gcm-id puk)]
          (println "GCM: sending gcm notification to" gcm-id)
          (let [response (<! (async-gcm-notify gcm-id))]
            (println "GCM:" response)
            ;; TODO: treat error codes here
            (if-some [retry-after-secs (-> response :headers (get "Retry-After"))]
              (recur puks (async/timeout (* 1000 (Integer/valueOf retry-after-secs))))
              (recur (disj puks puk) IMMEDIATELY))))))))

(defn start [port puks-in]

  (let [puk->gcm-id (atom {})]

    (start-gcm-notifier puk->gcm-id puks-in)

    (defroutes app
      (GET "/gcm/register" req (gcm-register puk->gcm-id req))
      (GET "/gcm/query" [] (str @puk->gcm-id))
      (route/not-found "<h1>page not found</h1>"))

    (http/run-server (-> app params/wrap-params) {:port port})))

;; for REPL usage
;(def puks-in (async/chan))
;(def server (start 8080 puks-in))
;
;(defn stop []
;  (async/close! puks-in)
;  (server))
;
;(defn notify [puk]
;  (async/go
;    (async/>! puks-in puk)))
;
;(notify (keys/->puk "foo"))
;
;(stop)