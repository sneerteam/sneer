(ns sneer.server.http-server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [sneer.keys :as keys]))

(defn gcm-register [state req]
  (let [params (:query-params req)
        puk (get params "puk")
        id (get params "id")]
    (assert (not (or (empty? puk) (empty? id))))
    (swap! state assoc (keys/->puk puk) id)
    (str "id for " puk " set to " id)))

(defn start [port]

  (let [state (atom {})]

    (defroutes app
               (GET "/gcm/register" req (gcm-register state req))
               (GET "/gcm/query" [] (str @state))
               (route/not-found "<h1>page not found</h1>"))

    (http/run-server (params/wrap-params app) {:port port})))


;;(def server (start 8080))

;;(defn stop [] (server))

;;(stop)