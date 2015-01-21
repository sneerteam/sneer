(ns sneer.server.prevalence
  (:refer-clojure :exclude [write read])
  (:require
    [sneer.serialization :refer [write writer read reader]]
    [sneer.commons :refer :all]
    [sneer.async :refer [go-trace]]
    [clojure.java.io :refer [file]]
    [clojure.core.async :refer [close! >! <!]])
  (:import
   [java.io File FileOutputStream FileInputStream]))

(defn- write-single! [filename item]
  (with-open [out-stream (FileOutputStream. filename)]
    (write (writer out-stream) item)))

(defn- replace! [^File old-file ^File new-file]
  (assert (and (.delete old-file) (.renameTo new-file old-file))))

(defn- read-ignoring-exception [reader]
  (try
    (read reader)
    (catch Exception end-of-stream)))

(defn logger [log-file replay log]
  
  (go-trace 
    
   ;; Replay
   (let [r (reader (FileInputStream. log-file))]
     (while-let [item (read-ignoring-exception r)]
       (>! replay item))
     (close! replay))
   
   ;; Overwrite on first write
   (when-some [first-item (<! log)]
     (let [tmp-file (file (str log-file ".tmp"))]
       (write-single! tmp-file first-item)
       (replace! log-file tmp-file)))
   
   ;; Append on subsequent writes
   (with-open [out-stream (FileOutputStream. log-file true)]
     (let [w (writer out-stream)]
       (while-let [item (<! log)]
         (write w item))))))
