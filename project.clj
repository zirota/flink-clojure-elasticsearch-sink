(def version {:elasticsearch "7.17.1"
              :flink         "1.15.0"})

(defproject org.clojars.hazzenc/flink-clojure-elasticsearch-sink "0.1.0-SNAPSHOT"
  :description "A Clojure wrapper for the Elasticsearch 7 Flink Connector"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.apache.flink/flink-connector-elasticsearch7 ~(:flink version)]
                 [org.apache.flink/flink-streaming-java ~(:flink version) :scope "provided"]]
  :main ^:skip-aot flink-clojure-elasticsearch-sink.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
