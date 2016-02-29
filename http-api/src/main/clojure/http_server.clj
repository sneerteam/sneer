(ns http-server
  (require [org.httpkit.server :refer :all]))
(defn async-handler [ring-request]
 ;; unified API for WebSocket and HTTP long polling/streaming
 (with-channel ring-request channel    ; get the channel
               (if (websocket? channel)            ; if you want to distinguish them
                (on-receive channel (fn [data]     ; two way communication
                                     (send! channel data)))
                (send! channel {:status 200
                                :headers {"Content-Type" "text/plain"}
                                :body    "Long polling?"}))))

(defn -main
  [& args]
  (run-server async-handler {:port 8080}))


