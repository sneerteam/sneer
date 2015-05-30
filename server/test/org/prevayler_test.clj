(ns org.prevayler-test
  (:require
    [org.prevayler :refer :all]
    [midje.sweet :refer :all])
  (:import
    [java.io File]))

; (do (require 'midje.repl) (midje.repl/autotest))

(defn- tmp-file []
  (doto
    (File/createTempFile "test-" ".tmp")
    (.delete)))

(fact "Prevalence"
  (try
    (let [handler-fn +
          initial-state 0
          file (tmp-file)

          prevayler! #(prevayler-jr! handler-fn initial-state file)]

      (let [p1 (prevayler!)]
        @p1 => 0
        (handle! p1 42)
        @p1 => 42
        (handle! p1 100)
        @p1 => 142
        (close! p1))

      ; Restart with same file (initial state saved as first item)
      (let [p2 (prevayler!)]
        @p2 => 142
        (handle! p2 1000)
        @p2 => 1142
        (close! p2))

      ; Restart with same file (previous state saved as first item)
      (let [p3 (prevayler!)]
        @p3 => 1142
        (handle! p3 10000)
        @p3 => 11142
        (close! p3))

      ; Simulate crash during restart.
      (assert (.renameTo file (backup-file file)))
      (spit file "#$@%@corruption&@#$@")
      (let [p4 (prevayler!)]
        @p4 => 11142
        (close! p4)))
    (catch Exception e (.printStackTrace e))))
