(ns sneer.io
  (:require [clojure.java.io :as io])
  (:import [java.io File]))

(defn read-bytes [^File file]
  (let [buffer (byte-array (.length file))]
    (.read (io/input-stream file) buffer)
    buffer))

(defn write-bytes [file buffer]
  (with-open [out (io/output-stream file)]
    (.write out buffer)))
