(ns http-server
  (require [org.httpkit.server :refer :all]
           [sneer.core2-sim :refer [dispatch! dispatcher]]
           [sneer.serialization :refer [deserialize]]
           [clojure.java.io :refer [reader]]))
(defn async-handler [ring-request]
 ;; unified API for WebSocket and HTTP long polling/streaming
 (with-channel ring-request channel    ; get the channel
               (if (websocket? channel)            ; if you want to distinguish them
                (on-receive channel (fn [data]     ; two way communication
                                     (println "socket data " (deserialize (.getBytes data)))
                                     (send! channel data)))
                (send! channel {:status 200
                                :headers {"Content-Type" "text/plain"}
                                :body    "Long polling?"}))))

(defn -main
  [& args]
  (run-server async-handler {:port 8080}))


