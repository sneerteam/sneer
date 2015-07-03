(ns sneer.time
  (:import [sneer.commons Clock]
           [java.util Date]
           [org.ocpsoft.prettytime PrettyTime]))

(defn pretty-printer []
  (let [pretty-time (PrettyTime. (Date. (Clock/now)))]
    (fn [^long timestamp]
      (.format pretty-time (Date. timestamp)))))
