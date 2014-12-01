(defproject core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [com.cognitect/transit-clj "0.8.229"]
                 [midje "1.6.3"]
                 [com.netflix.rxjava/rxjava-clojure "0.20.3"]
                 [me.sneer/java-api "0.0.1"]
                 [me.sneer/crypto "0.0.1"]]
  :profiles {:dev {:plugins [[cider/cider-nrepl "0.8.1"]]}}
  :plugins [[lein-midje "3.0.0"]]
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["src/test/clojure"])
