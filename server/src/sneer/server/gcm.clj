(ns sneer.server.gcm
  (:require [org.httpkit.client :as http]))

(def api-key "AIzaSyDkBjnfvd2bzp-qNDIWRLXSvRVy12KwdsU")

#_("

Content-Type:application/json
Authorization:key=AIzaSyB-1uEai2WiUapxCs2Q0GZYzPu7Udno5aA
{
  "registration_ids" : ["APA91bHun4MxP5egoKMwt2KZFBaFUH-1RYqx..."],
  "data" : {
          ...
          },
}
")

(defn send-msg []
  (http/post "https://android.googleapis.com/gcm/send"
    {:headers {"Content-Type" "application/json"
               "Authorization" "key=AIzaSyAqqgrLlio9WyDH2_Ztetxn82u4x_cvLfY"}
     :body "{
               \"registration_ids\": [ \"APA91bGdLsycUvLKLaWXw807-USTEBUcYE6p-D_758maR_iNQJpu_cb9-4NPDPe0HXimtGlZYLYACNsd_Imb5hQsFspdpJLOIhWknbxh1T7dqVimNr2pLoF_7_iya5RuYO5ksddp7YUjB7C_0n4bXzgDSJzf7tVtog\" ]
            }"
 }))
