(ns sneer.core-test
  (:require
   [clojure.core.async :as async :refer [chan >! <! >!! <!! alts!! timeout]]
   [clojure.test :refer :all]
   [midje.sweet :refer :all]
   [sneer.server.core :as core]
   [sneer.server.io :as io]))

(defn <?!! [c]
  (let [[v _] (alts!! [c (timeout 100)])]
    v))

(facts "Facts"
       (let [ca1 (chan 10)
             ca2 (chan 10)
             cb1 (chan 10)
             cb2 (chan 10)
             storage (io/create-temp-dir)
             _ (println storage)
             server (core/start-server! storage)
             ana-seal "1a1a"
             bob-seal "2b2b"
             ana (core/start-client! server ana-seal ca1 ca2)
             bob (core/start-client! server bob-seal cb1 cb2)]
         (core/sub ana [bob-seal ana-seal :notifications :chat :rooms] :sub-1)
         (core/pub bob [ana-seal :notifications :chat :rooms 42])
         (fact "Ana is notified of Bob's invite"
               (<?!! ca1) => {:path (list bob-seal ana-seal :notifications :chat :rooms 42)
                              :value :sneer.server.core/new
                              :id :sub-1
                              :tag :notification})

         (core/pub ana [:todo] :buy-the-milk)
         (Thread/sleep 100)
         (core/sub ana [ana-seal :todo] :sub-2)
         (fact "Sub can see previous pubs"
               (<?!! ca1) => {:path (list ana-seal :todo)
                              :value :buy-the-milk
                              :id :sub-2
                              :tag :notification})

         (core/pub ana [:notes :urgent] :buy-the-milk)
         (Thread/sleep 100)
         (core/sub ana [ana-seal :notes] :sub-3)
         (fact "Sub can see previous pubs of paths"
               (<?!! ca1) => {:path (list ana-seal :notes :urgent)
                              :value :sneer.server.core/new
                              :id :sub-3
                              :tag :notification}
               (<?!! ca1) => nil)

         (core/sub ana [ana-seal :notes] :sub-3)
         (fact "Subs with same id from same client are ignored"
               (<?!! ca1) => nil)

         (core/sub bob [ana-seal :notes] :sub-3)
         (fact "But subs with same id from different client are allowed"
               (<?!! cb1) => {:path (list ana-seal :notes :urgent)
                              :value :sneer.server.core/new
                              :id :sub-3
                              :tag :notification})))
