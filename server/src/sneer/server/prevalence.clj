(ns sneer.server.prevalence
  (:refer-clojure :exclude [write read])
  (:require
    [sneer.serialization :refer [write writer read reader]]
    [sneer.commons :refer :all])
  (:import
   [java.io File FileOutputStream FileInputStream]))

(defprotocol Prevayler
  (handle! [_ event])
  (close! [_]))

(defn- read-ignoring-exception [reader]
  (try
    (read reader)
    (catch Exception end-of-stream
      (println "EXCEPTION AT END OF LOG:" end-of-stream))))

(defn- archive [^File file]
  (.renameTo file (File. (str file "-" (java.lang.System/currentTimeMillis)))))

(defn replacement [file]
  (File. (str file ".replacement")))

(defn- atomic-replace! [file content]
  (let [^File rep (replacement file)]
    (with-open [out (FileOutputStream. rep)]
	    (write (writer out) content))
    (archive file)
    (assert (.renameTo rep file))))

(defn- atomic-recover! [^File file]
  (let [^File rep (replacement file)]
    (when-not (.exists file)
      (assert
        (if (.exists rep)
          (.renameTo rep file)
          (.createNewFile file))))))

(defn prevayler-jr!
  ([handler initial-state]
    (let [state (atom initial-state)]
      (reify
        Prevayler
          (handle! [_ event]
            (swap! state handler event))
          (close! [_]
            (reset! state ::closed))
        clojure.lang.IDeref
          (deref [_]
            @state))))
  
  ([handler initial-state file]
    (let [state (atom initial-state)
          -handle! (partial swap! state handler)]
      (atomic-recover! file)
      (with-open [in (FileInputStream. file)]
        (let [r (reader in)]
          (when-let [previous (read-ignoring-exception r)]
            (reset! state previous))
          (while-let [event (read-ignoring-exception r)]
            (-handle! event))))

      (atomic-replace! file @state)
    
      (let [append true
            out (FileOutputStream. file append)
            w (writer out)]
        (reify
          Prevayler
            (handle! [_ event]
              (write w event)
              (-handle! event))
            (close! [_]
              (reset! state ::closed)
              (.close out))
          clojure.lang.IDeref
            (deref [_]
              @state))))))