(defproject sneer.server "0.1.0-SNAPSHOT"
  :description "The Sneer Temporary Server"
  :dependencies [[me.sneer/java-api "0.0.1"]
                 [me.sneer/core "0.0.1"]
                 [com.taoensso/timbre "3.2.1"] ;Logging
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot sneer.server.main-new
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[cider/cider-nrepl "0.8.2"]
                             [lein-midje "3.1.3"]]}})
