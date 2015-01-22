(ns sneer.server.prevalence
  (:refer-clojure :exclude [write read])
  (:require
    [sneer.serialization :refer [write writer read reader]]
    [sneer.commons :refer :all])
  (:import
   [java.io File FileOutputStream FileInputStream]))

(defn- read-ignoring-exception [reader]
  (try
    (read reader)
    (catch Exception end-of-stream
      (println "EXCEPTION AT END OF LOG:" end-of-stream))))

(defn- archive [file]
  (.renameTo file (File. (str file "-" (java.lang.System/currentTimeMillis)))))

(defn replacement [file]
  (File. (str file ".replacement")))

(defn- atomic-replace! [file content]
  (let [rep (replacement file)]
    (with-open [out (FileOutputStream. rep)]
	    (write (writer out) content))
    (archive file)
    (assert (.renameTo rep file))))

(defn- atomic-recover! [file]
  (let [rep (replacement file)]
    (when-not (.exists file)
      (assert
        (if (.exists rep)
          (.renameTo rep file)
          (.createNewFile file))))))

(defn state [prevayler]
  @(:state prevayler))

(defn handle! [prevayler event]
  (write (:writer prevayler) event)
  (let [handler (:handler prevayler)]
    (swap! (:state prevayler) handler event)))

(defn close! [prevayler]
  (.close (:output-stream prevayler)))

(defn prevayler-jr! [file initial-state handler]
  (let [state (atom initial-state)]
    (atomic-recover! file)
    (with-open [in (FileInputStream. file)]
      (let [r (reader in)]
        (when-let [previous (read-ignoring-exception r)]
          (reset! state previous))
        (while-let [event (read-ignoring-exception r)]
          (swap! state handler event))))

  (atomic-replace! file @state)
    
  (let [out (FileOutputStream. file true)
        w (writer out)]
    {:output-stream out
     :writer w
     :state state
     :handler handler})))