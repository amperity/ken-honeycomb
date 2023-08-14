(defproject com.amperity/ken-honeycomb "1.2.1-SNAPSHOT"
  :description "Observability library to integrate ken and honeycomb"
  :url "https://github.com/amperity/ken-honeycomb"
  :license {:name "MIT License"
            :url "https://mit-license.org/"}

  :plugins
  [[lein-cloverage "1.2.2"]]

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [com.amperity/ken "1.2.0"]
   [com.stuartsierra/component "1.1.0"]
   [io.honeycomb.libhoney/libhoney-java "1.5.4"]]

  :profiles
  {:dev
   {:dependencies
    [[org.slf4j/slf4j-nop "2.0.7"]]}

   :repl
   {:source-paths ["dev"]
    :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
    :dependencies
    [[org.clojure/tools.namespace "1.4.4"]]}})
