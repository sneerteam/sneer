(ns sneer.server.gcm
  (:require [org.httpkit.client :as http]))

(def api-key "AIzaSyDkBjnfvd2bzp-qNDIWRLXSvRVy12KwdsU")

(defn send-to [id callback]
  (http/post "https://android.googleapis.com/gcm/send"
             {:headers {"Content-Type" "application/json"
                        "Authorization" (str "key=" api-key)}
              :timeout 3000
              :body (str "{\"registration_ids\": [ \"" id "\" ]
                          ,\"collapse_key\"    : \"wakeup\"}")}
             callback))
