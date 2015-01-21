(ns sneer.server.prevalence
  (:require
    [sneer.serialization :refer [write writer]]
    [sneer.commons :refer :all]
    [sneer.async :refer [go-trace]]
    [clojure.core.async :refer [close!]]))

(defn- file-with! [filename item]
  (with-open [out-stream (FileOutputStream. filename)]
    (write (writer out-stream) item)))

(defn- replace! [old-file new-file]
  )

(defn logger [file replay log]
  (go-trace
    (close! replay)
    (when-some [first-item (<! log)]
      (let [tmp-file (file-with! (str file ".tmp") first-item)]
        (replace! file tmp-file)))
    (with-open [out-stream (FileOutputStream. file true)]
      (let [w (writer out-stream)]
        (while-let [item (<! log)]
          (write w item))))))
