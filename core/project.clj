(defproject me.sneer/core "0.1.5"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.skummet/clojure "1.7.0-alpha5-r1"]
                 [me.sneer/sneer-java-api "0.1.5"]
                 [me.sneer/crypto "0.1.5"]
                 [org.clojure/core.match "0.2.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.cognitect/transit-clj "0.8.259"]
                 [com.netflix.rxjava/rxjava-core "0.20.7"]
                 [org.clojure/java.jdbc "0.3.6"]
                                        ;[midje "1.6.3"]
                                        ;[org.xerial/sqlite-jdbc "3.8.6"]
                 ]


  :exclusions [[org.clojure/clojure]]

  :omit-source true

  :global-vars {clojure.core/*warn-on-reflection* true}

  :skummet-skip-vars []

  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :aot [
        sneer.restartable
        sneer.party-impl
        sneer.commons
        sneer.tuple.protocols
        sneer.rx-macros
        sneer.rx
        sneer.async
        sneer.tuple-base-provider
        sneer.tuple.persistent-tuple-base
        sneer.tuple.space
        sneer.tuple.queue
        sneer.tuple.tuple-transmitter
        sneer.admin
        sneer.contact
        sneer.conversation
        sneer.impl
        sneer.keys
        sneer.party
        sneer.profile
        sneer.serialization
        sneer.networking.udp
        sneer.networking.client
        sneer.main
        ]
  :uberjar-exclusions [#"META-INF/DUMMY.SF"
                       #"^org/(apache|bouncycastle|json|msgpack|objectweb)"
                       #"^clojure/test/"]

  :jvm-opts ["-Dclojure.compile.ignore-lean-classes=true"]
  :profiles {:default []}
                                        ;  :profiles  {:dev {:plugins [[cider/cider-nrepl "0.8.2"][refactor-nrepl "0.2.2"]]}}
  :plugins [                            ;[lein-midje "3.0.0"]
            [org.skummet/lein-skummet "0.2.1"]]
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["src/test/clojure"])
