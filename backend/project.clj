(defproject app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238" :scope "provided"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [bk/ring-gzip "0.3.0"]
                 [radicalzephyr/ring.middleware.logger "0.6.0"]
                 [clj-logging-config "1.9.12"]
                 [compojure "1.6.1"]
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.danielsz/system "0.4.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [rum "0.11.2"]

                 [gamebase "0.1.1-SNAPSHOT"]
                 ;; AND THESE ARE ALSO FOR gamebase:
                 [org.clojure/data.xml "0.2.0-alpha2"]
                 [hiccup "1.0.5"]
                 [com.taoensso/carmine "2.16.0"]

                 ]

  :plugins [[lein-environ "1.1.0"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} [:target-path :compile-path "dev-target"]

  :uberjar-name "app.jar"

  ;; Use `lein run` if you just want to start a HTTP server, without figwheel
  :main app.application

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (go) and
  ;; (browser-repl) live.
  :repl-options {:init-ns user}

  ;; When running figwheel from nREPL, figwheel will read this configuration
  ;; stanza, but it will read it without passing through leiningen's profile
  ;; merging. So don't put a :figwheel section under the :dev profile, it will
  ;; not be picked up, instead configure figwheel here on the top level.

  :profiles {:dev
             {:dependencies [[org.clojure/tools.nrepl "0.2.13"]
                             [reloaded.repl "0.2.4"]]
              :source-paths ["dev"]
              :repl-options {}}

             :uberjar
             {:source-paths ^:replace ["src/clj"]
              :prep-tasks ["compile"]
              :hooks []
              :omit-source true
              :aot :all}})
