(defproject com.amperity/ken-honeycomb "1.0.1"
  :description "Observability library to integrate ken and honeycomb"
  :url "https://github.com/amperity/ken-honeycomb"
  :license {:name "MIT License"
            :url "https://mit-license.org/"}

  :plugins
  [[lein-cloverage "1.2.2"]]

  :dependencies
  [[org.clojure/clojure "1.10.3"]
   [com.amperity/ken "1.0.1"]
   [com.stuartsierra/component "1.0.0"]
   [io.honeycomb.libhoney/libhoney-java "1.4.0"]]

  :profiles
  {:dev
   {:dependencies
    [[org.slf4j/slf4j-nop "1.7.32"]]}

   :repl
   {:source-paths ["dev"]
    :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
    :dependencies
    [[org.clojure/tools.namespace "1.2.0"]]}})
