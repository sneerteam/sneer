(defproject reagent-spike "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring-server "0.4.0"]
                 [reagent "0.5.1"
                  :exclusions [org.clojure/tools.reader]]
                 [reagent-forms "0.5.21"]
                 [reagent-utils "0.1.7"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.5.0"]
                 [hiccup "1.0.5"]
                 [environ "1.0.2"]
                 [org.clojure/clojurescript "1.7.228"
                  :scope "provided"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.1.7"
                  :exclusions [org.clojure/tools.reader]]
                 [com.cognitect/transit-cljs "0.8.237"]
                 [org.clojure/core.async    "0.2.374"]
                 [com.taoensso/sente        "1.8.1"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-cljsbuild "1.1.1"]
            [lein-asset-minifier "0.2.7"
             :exclusions [org.clojure/clojure]]]

  :ring {:handler reagent-spike.handler/app
         :uberwar-name "reagent-spike.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "reagent-spike.jar"

  :main reagent-spike.server

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :compiler {:output-to "target/cljsbuild/public/js/app.js"
                                        :output-dir "target/cljsbuild/public/js/out"
                                        :asset-path   "/js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}


  :profiles {:dev {:repl-options {:init-ns reagent-spike.repl}

                   :dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.4.0"]
                                  [prone "1.0.2"]
                                  [lein-figwheel "0.5.0-6"
                                   :exclusions [org.clojure/core.memoize
                                                ring/ring-core
                                                org.clojure/clojure
                                                org.ow2.asm/asm-all
                                                org.clojure/data.priority-map
                                                org.clojure/tools.reader
                                                org.clojure/clojurescript
                                                org.clojure/core.async
                                                org.clojure/tools.analyzer.jvm]]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [pjstadig/humane-test-output "0.7.1"]
                                  ]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.0-6"
                              :exclusions [org.clojure/core.memoize
                                           ring/ring-core
                                           org.clojure/clojure
                                           org.ow2.asm/asm-all
                                           org.clojure/data.priority-map
                                           org.clojure/tools.reader
                                           org.clojure/clojurescript
                                           org.clojure/core.async
                                           org.clojure/tools.analyzer.jvm]]
                             ]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :nrepl-port 7002
                              :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                                                 ]
                              :css-dirs ["resources/public/css"]
                              :ring-handler reagent-spike.handler/app}

                   :env {:dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "reagent-spike.dev"
                                                         :source-map true}}



                                        }
                               }}

             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
