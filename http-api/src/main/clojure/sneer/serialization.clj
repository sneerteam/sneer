(ns sneer.serialization
 (:refer-clojure :exclude [read write])
 (:import
  [java.io ByteArrayInputStream ByteArrayOutputStream])
 (:require [cognitect.transit :as transit]))

(def ^:private transit-format :json) ; other options are :json-verbose and :msgpack


(def write transit/write)
(def read  transit/read)

(defn writer [output-stream]
 (transit/writer output-stream transit-format))

(defn reader [input-stream]
 (transit/reader input-stream  transit-format))

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
