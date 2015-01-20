(ns sneer.serialization
  (:import
    [java.io ByteArrayInputStream ByteArrayOutputStream]
    [sneer PublicKey]
    [sneer.crypto.impl KeysImpl]
    [sneer.commons.exceptions FriendlyException])
  (:require [cognitect.transit :as transit]))

(def ^:private transit-format :json) ; other options are :json-verbose and :msgpack

(def ^:private write-handlers
  {PublicKey
   (transit/write-handler
     (fn [_] "puk")
     (fn [^PublicKey puk] (.toBytes puk)))})

(def ^:private read-handlers
  (let [keys-impl (KeysImpl.)]
    {"puk" (transit/read-handler
             (fn [^bytes rep] (.createPublicKey keys-impl rep)))}))

(def ^:private write-opts {:handlers write-handlers})

(def ^:private read-opts {:handlers read-handlers})

(defn serialize [value]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out transit-format write-opts)]
    (transit/write writer value)
    (.toByteArray out)))

(defn deserialize
  ([^bytes bytes]
     (deserialize bytes (alength bytes)))
  ([bytes length]
     (let [in (ByteArrayInputStream. bytes 0 length)
           reader (transit/reader in transit-format read-opts)]
       (transit/read reader))))

(defn roundtrip [value max-size]
  (let [bytes (serialize value)
        size (alength bytes)]
    (when (> size max-size)
      (throw (FriendlyException. (str "Value too large (" size " bytes). Maximum is " max-size " bytes."))))
    (deserialize bytes)))
