(ns sneer.invite
  [:require
    [clojure.data.codec.base64 :as base64]
    [sneer.util.msgpack :as msg]])

(def ^:private version 1)

(defn- ->string [^bytes bytes]
  (String. bytes "UTF8"))

(defn encode [{:keys [puk name nonce]}]
  (assert (and puk name nonce))
  (let [vector [version puk name nonce]]
    (-> vector
      msg/pack
      base64/encode
      ->string)))

(defn decode [invite-string]
  (let [vector (-> invite-string
                 (.getBytes "UTF8")
                 (base64/decode)
                 (msg/unpack))
        [version] vector]
    (assert (= version 1) "You need to update Sneer to accept this invite")
    (let [[_ puk name] vector]
      {:puk  puk
       :name name})))