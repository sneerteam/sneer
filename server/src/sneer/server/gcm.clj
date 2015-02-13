(ns sneer.server.gcm
  (:require [org.httpkit.client :as http]
            [clojure.string :refer [trim]]))

(def api-key
  (let [currentDir (-> "whatever" java.io.File. .getCanonicalFile .getParentFile .getName)]
    (if (= currentDir "sneer-live")
      (-> "google-api.key" slurp trim)
      "CGM-is-not-authorized-outside-the-server")))

(defn send-to [id callback]
  (http/post "https://android.googleapis.com/gcm/send"
             {:headers {"Content-Type" "application/json"
                        "Authorization" (str "key=" api-key)}
              :timeout 3000
              :body (str "{\"registration_ids\": [ \"" id "\" ]
                          ,\"collapse_key\"    : \"wakeup\"}")}
             callback))
