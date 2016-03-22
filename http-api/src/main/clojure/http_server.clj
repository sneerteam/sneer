(ns http-server
  (require [org.httpkit.server :refer :all]
           [sneer.core2-sim :refer :all]
           [sneer.serialization :refer [serialize deserialize]]
           [clojure.java.io :refer [reader]]))

(def current-channel (atom nil))

(defn ui-fn [view]
  (let [response (String. ^bytes (serialize view))]
    (println "> " response)
    (send! @current-channel response)))

(def sneer (sneer-simulator ui-fn))

(defn handleEvent! [channel event]
  (println "< " event)
  (reset! current-channel channel)
  (handle! sneer event))

(defn async-handler [ring-request]
 ;; unified API for WebSocket and HTTP long polling/streaming
 (with-channel ring-request channel    ; get the channel
               (if (websocket? channel)            ; if you want to distinguish them
                (on-receive channel
                            (fn [data]     ; two way communication
                                      (handleEvent! channel (deserialize (.getBytes data)))
                  ))
                (send! channel {:status 200
                                :headers {"Content-Type" "text/plain"}
                                :body    "Long polling?"}))))



(defn -main
  [& args]
  (let [port 8080]
    (println "Sneer server running on " port)
    (run-server async-handler {:port port})))


