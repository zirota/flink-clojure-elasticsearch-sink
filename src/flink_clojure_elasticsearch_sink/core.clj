(ns flink-clojure-elasticsearch-sink.core
  (:require
    [clojure.data.json :as json]
    [clojure.string :as str])
  (:import
    (java.net URL)
    (org.apache.flink.api.connector.sink2 SinkWriter$Context)
    (org.apache.flink.connector.base DeliveryGuarantee)
    (org.apache.flink.connector.elasticsearch.sink Elasticsearch7SinkBuilder ElasticsearchEmitter FlushBackoffType RequestIndexer)
    (org.apache.http HttpHost)
    (org.elasticsearch.action DocWriteRequest$OpType)
    (org.elasticsearch.action.index IndexRequest)
    (org.elasticsearch.common.xcontent XContentType))
  (:gen-class))

(def ^{:doc
       "Can be rebound using binding or alter-var-root to ElasticsearchEmitter with reify.
        This can be useful for retrieving the context of the ElasticsearchEmitter or
        adding additional parameters to the IndexRequest."
       :dynamic true}
  *elasticsearch-emitter-fn*)

(defn- urls->http-hosts [urls]
  (->> (str/split urls #",")
       (map (fn [url-str]
              (let [url (URL. url-str)
                    ^String host (.getHost url)
                    ^String scheme (.getProtocol url)
                    ^int port (if (= (.getPort url) -1)
                                9200
                                (.getPort url))]
                (HttpHost. host port scheme))))
       (reduce conj [])))

(defn ->index-request [{:keys [id
                               index
                               create?
                               source
                               version
                               version-type
                               ^DocWriteRequest$OpType op-type
                               ^XContentType content-type
                               routing
                               pipeline
                               final-pipeline
                               pipeline-resolved?
                               primary-term
                               seq-no
                               require-alias?]
                        :or   {create?      false
                               content-type XContentType/JSON}}]
  (doto (IndexRequest.)
    (.id id)
    (.index index)
    (.source source content-type)
    (.create create?)
    (.version version)
    (.version version-type)
    (.opType op-type)
    (.routing routing)
    (.setPipeline pipeline)
    (.setFinalPipeline final-pipeline)
    (.isPipelineResolved pipeline-resolved?)
    (.setIfPrimaryTerm primary-term)
    (.setIfSeqNo seq-no)
    (.setRequireAlias require-alias?)))

(defn- ->elasticsearch-emitter []
  (when (not (bound? #'*elasticsearch-emitter-fn*))
    (alter-var-root
      #'*elasticsearch-emitter-fn*
      (constantly (reify ElasticsearchEmitter
                    (^void emit [this record ^SinkWriter$Context context ^RequestIndexer indexer]
                      (let [index-id (:index-id record)
                            doc (some-> (:source record)
                                        (json/json-str :key-fn name))
                            doc-id (:doc-id record)
                            index-request (->index-request {:id     doc-id
                                                            :index  index-id
                                                            :source doc})]
                        (.add indexer (into-array IndexRequest (index-request))))))))))

(defn ->elasticsearch-sink [{:keys [bulk-flush-max-actions
                                    bulk-flush-max-size-mb
                                    bulk-flush-interval
                                    connection-password
                                    connection-path-prefix
                                    connection-request-timeout
                                    connection-timeout
                                    connection-username
                                    ^DeliveryGuarantee delivery-guarantee
                                    delay-millis
                                    ^FlushBackoffType flush-backoff-type
                                    hosts
                                    max-retries
                                    socket-timeout]
                             :or   {bulk-flush-max-actions 64
                                    bulk-flush-interval    5000
                                    flush-backoff-type     FlushBackoffType/EXPONENTIAL
                                    max-retries            3
                                    delay-millis           5000}}]
  (-> (Elasticsearch7SinkBuilder.)
      (.setBulkFlushBackoffStrategy flush-backoff-type max-retries delay-millis)
      (.setBulkFlushInterval bulk-flush-interval)
      (.setBulkFlushMaxActions bulk-flush-max-actions)
      (.setBulkFlushMaxSizeMb bulk-flush-max-size-mb)
      (.setConnectionPassword connection-password)
      (.setConnectionPathPrefix connection-path-prefix)
      (.setConnectionRequestTimeout connection-request-timeout)
      (.setConnectionTimeout connection-timeout)
      (.setConnectionUsername connection-username)
      (.setDeliveryGuarantee delivery-guarantee)
      (.setHosts (into-array HttpHost hosts))
      (.setEmitter (->elasticsearch-emitter))
      (.setSocketTimeout socket-timeout)
      (.build)))
