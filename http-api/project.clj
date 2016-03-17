(defproject me.sneer/http-api "0.0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :warn-on-reflection false

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [me.sneer/core2 "0.0.1-SNAPSHOT"]
                 [http-kit "2.1.18"]
                 [com.cognitect/transit-clj "0.8.285"]]

  :main http-server

  :profiles {
             :dev
             {:dependencies [[midje "1.7.0"]]
              :plugins [[lein-midje "3.1.3"]]}}


  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"])
