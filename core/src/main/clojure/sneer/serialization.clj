(ns sneer.serialization
  (:refer-clojure :exclude [read write])
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

(def write transit/write)
(def read  transit/read)

(defn writer [output-stream]
  (transit/writer output-stream transit-format write-opts))

(defn reader [input-stream]
  (transit/reader input-stream  transit-format read-opts))

(defn serialize [value]
  (let [out (ByteArrayOutputStream.)]
    (write (writer out) value)
    (.toByteArray out)))

(defn deserialize
  ([^bytes bytes]
     (deserialize bytes (alength bytes)))
  ([bytes length]
     (let [in (ByteArrayInputStream. bytes 0 length)]
       (read (reader in)))))

(defn roundtrip [value max-size]
  (let [bytes (serialize value)
        size (alength bytes)]
    (when (> size max-size)
      (throw (FriendlyException. (str "Value too large (" size " bytes). Maximum is " max-size " bytes."))))
    (deserialize bytes)))
