(ns sneer.serialization
  (:import
    [java.io ByteArrayInputStream ByteArrayOutputStream]
    [sneer PublicKey]
    [sneer.impl.keys Keys])
  (:require [cognitect.transit :as transit]))

(def ^:private transit-format :json) ; other options are :json-verbose and :msgpack

(def ^:private write-handlers
  {PublicKey
   (transit/write-handler
     (fn [_] "puk")
     (fn [puk] (.bytes puk)))})

(def ^:private read-handlers
  {"puk"
   (transit/read-handler
     (fn [rep] (Keys/createPublicKey rep)))})

(def ^:private write-opts {:handlers write-handlers})

(def ^:private read-opts {:handlers read-handlers})

(defn serialize [value]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out transit-format write-opts)]
    (transit/write writer value)
    (.toByteArray out)))

(defn deserialize [bytes]
  (let [in (ByteArrayInputStream. bytes)
        reader (transit/reader in transit-format read-opts)]
    ;(println (alength bytes) (String. bytes "utf8"))
    (transit/read reader)))