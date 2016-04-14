(ns example.server
  "Official Sente reference example: server"
  {:author "Peter Taoussanis (@ptaoussanis)"}

  (:require
   [clojure.string     :as str]
   [ring.middleware.defaults]
   [compojure.core     :as comp :refer (defroutes GET POST)]
   [compojure.route    :refer [resources]]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.encore    :as encore :refer ()]
   [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente     :as sente]

   [org.httpkit.server :as http-kit]
   [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]

   ;; Optional, for Transit encoding:
   [taoensso.sente.packers.transit :as sente-transit]

   [sneer.core2-sim :refer :all]))

;; (timbre/set-level! :trace) ; Uncomment for more logging


(defn start-selected-web-server! [ring-handler port]
  (infof "Starting http-kit...")
  (let [stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server  nil ; http-kit doesn't expose this
     :port    (:local-port (meta stop-fn))
     :stop-fn (fn [] (stop-fn :timeout 100))}))


;;;; Define our Sente channel socket (chsk) server

(let [;; Serializtion format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-flexi-packer :edn) ; Experimental, needs Transit dep

      {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket-server! sente-web-server-adapter
        {:packer packer})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

;;;; Ring handlers


(defroutes ring-routes
  (GET  "/chsk"  ring-req (ring-ajax-get-or-ws-handshake ring-req))
  (POST "/chsk"  ring-req (ring-ajax-post                ring-req))
  (resources "/"))

(def main-ring-handler
  "**NB**: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
  middleware to work. These are included with
  `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
  that they're included yourself if you're not using `wrap-defaults`."
  (ring.middleware.defaults/wrap-defaults
    ring-routes ring.middleware.defaults/site-defaults))

;;;; Sneer simulator

(def fake-ui  (atom nil))
(def simulator  (sneer-simulator #(reset! fake-ui %)))

;;;; Sente event handlers

(defn -event-msg-handler
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid (:uid session)]

    (if (= :sneer/handle (first event))
      (do
        (handle! simulator (second event))
        (debugf "Fake UI: %s" @fake-ui))

      (debugf "Unhandled event: %s" event))
    (comment (when ?reply-fn
               (?reply-fn {:umatched-event-as-echoed-from-from-server event})))
    ))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk -event-msg-handler)))

;;;; Some server>user async push examples

(defn test-fast-server>user-pushes
  "Quickly pushes 100 events to all connected users. Note that this'll be
  fast+reliable even over Ajax!"
  []
  (doseq [uid (:any @connected-uids)]
    (doseq [i (range 100)]
      (chsk-send! uid [:fast-push/is-fast (str "hello " i "!!")]))))

(comment (test-fast-server>user-pushes))

;;;; Init stuff

(defn start-web-server! [& [port]]
  (let [{:keys [port]}
        (start-selected-web-server! (var main-ring-handler)
          (or port 0) ; 0 => auto (any available) port
          )
        uri (format "http://localhost:%s/main.html" port)]
    (infof "Web server is running at `%s`" uri)))

(defn start! [] (start-router!) (start-web-server! 8888))
(defn -main "For `lein run`, etc." [] (start!))