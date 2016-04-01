(defproject me.sneer/core2 "0.0.1-SNAPSHOT"
  :description "Sneer core features"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.codec "0.1.0"] ;Base64
                 [com.cognitect/transit-clj "0.8.285"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]}})
