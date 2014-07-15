(defproject sneer.server "0.1.0-SNAPSHOT"
  :description "The Sneer Temporary Server"
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"] ; We support Android 4.0.3 (API Level 15) or newer.
  :dependencies [[sneer/networker "0.1.5"]
                 [amalloy/ring-buffer "1.0"]
                 [com.taoensso/timbre "3.2.1"] ;Logging
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot sneer.server.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.2"]]}})
