(ns sneer.util.msgpack
  (:import
    [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:require [cognitect.transit :as t]))

(defn- writer [output-stream]
  (t/writer output-stream :msgpack))

(defn- reader [input-stream]
  (t/reader input-stream :msgpack))

(defn pack [value]
  (let [out (ByteArrayOutputStream.)]
    (t/write (writer out) value)
    (.toByteArray out)))

(defn unpack
  ([^bytes bytes]
     (unpack bytes (alength bytes)))
  ([bytes length]
     (let [in (ByteArrayInputStream. bytes 0 length)]
       (t/read (reader in)))))
