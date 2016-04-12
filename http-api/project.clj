(defproject me.sneer/http-api "0.0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.3.3"
  :global-vars {*warn-on-reflection* false
                *assert* true}

  :dependencies
  [
   [org.clojure/clojure       "1.8.0"]

   [org.clojure/clojurescript "1.7.170"]
   [org.clojure/core.async    "0.2.374"]

   [com.taoensso/sente        "1.8.1"]
   [com.taoensso/timbre       "4.3.1"]

   [http-kit                  "2.2.0-alpha1"]

   [ring                      "1.4.0"]
   [ring/ring-defaults        "0.2.0"]

   [compojure                 "1.5.0"]

   [com.cognitect/transit-clj  "0.8.285"]]

  :plugins
  [[lein-pprint         "1.1.2"]
   [lein-ancient        "0.6.8"]
   [com.cemerick/austin "0.1.6"]
   ]

  ; Temporary setup resource-paths (used by compojure.route/resources)
  ; Static html and css files:
  ;     spikes/reagend-spike/resources/public/main.html    -> /main.html
  ;     spikes/reagend-spike/resources/public/css/site.css -> /css/site.css
  ; Dynamic clojurescript files:
  ;     ../spikes/reagent-spike/target/cljsbuild/js/app.js -> /js/app.js
  :resource-paths ["../spikes/reagent-spike/resources" "../spikes/reagent-spike/target/cljsbuild"]

  :main example.server

  :repositories
  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
