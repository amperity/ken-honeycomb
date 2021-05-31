(ns ken.honeycomb
  "Integration support for publishing ken events to honeycomb.io."
  (:require
    [clojure.set :as set]
    [clojure.walk :as walk]
    [com.stuartsierra.component :as component]
    [ken.event :as event]
    [ken.tap :as tap]
    [ken.trace :as trace])
  (:import
    (io.honeycomb.libhoney
      Event
      HoneyClient
      LibHoney
      Options
      ResponseObserver)
    java.net.URI))


;; ## Field Formatting

(defn rename-default-fields
  "Transforming function which renames some ken keywords to match the default
  Honeycomb schema."
  [event]
  (set/rename-keys
    event
    {::event/label     :name
     ::event/level     :level
     ::event/message   :message
     ::event/duration  :duration_ms
     ;; ???            :service_name
     ::trace/trace-id  :trace.trace_id
     ::trace/parent-id :trace.parent_id
     ::trace/span-id   :trace.span_id}))


(defn- format-throwable
  "Convert a `Throwable` value into a structured map describing the error."
  [t]
  (cond-> {:class (.getName (class t))
           :message (ex-message t)}
    (ex-data t)
    (assoc :data (ex-data t))

    (ex-cause t)
    (assoc :cause (ex-cause t))))


(defn- format-value
  "Format a value in an event into a type that Honeycomb will understand.
  Defaults to stringifying unknown types."
  [x]
  ;; TODO: validate that map keys are strings?
  (cond
    ;; Many primitive types can generally be represented directly in JSON.
    (or (nil? x)
        (boolean? x)
        (string? x)
        (and (number? x)
             (not (ratio? x))))
    x

    ;; Concrete collection types can also be represented directly in JSON.
    ;; Sets, lists, and vectors will become arrays, while maps become objects.
    (or (map? x)
        (set? x)
        (list? x)
        (vector? x))
    x

    ;; Ensure we don't try to realize infinitely long lazy sequences.
    (seq? x)
    (take 1000 x)

    ;; Stringify keywords by removing the leading colon.
    (keyword? x)
    (subs (str x) 1)

    ;; Coerce ratios to floating-point numbers for JSON compatibility.
    (ratio? x)
    (double x)

    ;; Special formatting for error values.
    (instance? Throwable x)
    (format-throwable x)

    ;; Default to stringifying everything else.
    :else
    (str x)))


(defn- format-fields
  "Process a ken event to coerce some data types into data types that Honeycomb
  can handle as well as perform some data cleanup on the event."
  [transform event]
  (when-let [fields (-> event
                        (dissoc ::event/time ::event/sample-rate)
                        (transform)
                        (not-empty))]
    (walk/prewalk format-value fields)))


(defn- create-event
  "Create a Honeycomb Event object from a ken event."
  ^Event
  [^HoneyClient honeyclient transform data]
  (let [event (.createEvent honeyclient)
        fields (format-fields transform data)]
    (when (seq fields)
      (.addFields event fields)
      (when-let [timestamp (::event/time data)]
        (.setTimestamp event (inst-ms timestamp)))
      (when-let [sample-rate (::event/sample-rate data)]
        (.setSampleRate event (long sample-rate)))
      ;; TODO: it's possible for events to override some of the client
      ;; properties; how should this be exposed?
      ;; - ApiHost
      ;; - Dataset
      ;; - Metadata
      ;; - WriteKey
      event)))


(defn- send!
  "Records a ken event and sends a Honeycomb Event. Subscribe this function to
  the ken tap to connect events to honeycomb.io."
  [honeyclient transform data]
  (when-not (false? (::trace/keep? data))
    (when-let [event (create-event honeyclient transform data)]
      (.send event))))


;; ## Client Construction

(defn- client-options
  "Construct an `Options` object for initializing a `HoneyClient` from a map of
  values."
  ^Options
  [{:keys [api-host
           dataset
           sample-rate
           writekey]}]
  (->
    (LibHoney/options)
    (cond->
      api-host
      (.setApiHost (URI. api-host))

      dataset
      (.setDataset dataset)

      sample-rate
      (.setSampleRate sample-rate)

      writekey
      (.setWriteKey writekey))
    (.build)))


(defn- response-observer
  "Take the map of functions passed in the `:response-observer` client option
  and turn it into a `ResponseObserver` to attach to the HoneyClient."
  [{:keys [on-client-rejected
           on-server-accepted
           on-server-rejected
           on-unknown]}]
  (reify ResponseObserver
    (onClientRejected
      [_this event]
      (when on-client-rejected
        (on-client-rejected event)))

    (onServerAccepted
      [_this event]
      (when on-server-accepted
        (on-server-accepted event)))

    (onServerRejected
      [_this event]
      (when on-server-rejected
        (on-server-rejected event)))

    (onUnknown
      [_this event]
      (when on-unknown
        (on-unknown event)))))


(defn- init-honeyclient
  "Initialize a HoneyClient with options."
  ^HoneyClient
  [options]
  (when-not (map? options)
    (throw (IllegalArgumentException. "HoneyClient options must be a map.")))
  (let [opts (client-options options)
        client (HoneyClient. opts)]
    (when-let [callbacks (not-empty (:response-observer options))]
      (.addResponseObserver client (response-observer callbacks)))
    (.closeOnShutdown client)
    client))


;; ## Observer Component

(defrecord HoneyObserver
  [^HoneyClient client dataset writekey transform]

  component/Lifecycle

  (start
    [this]
    (let [client (init-honeyclient this)]
      (tap/subscribe! ::send (partial send! client (or transform identity)))
      (assoc this :client client)))


  (stop
    [this]
    (tap/unsubscribe! ::send)
    (when client
      (.close client))
    (assoc this :client nil)))


(defn honey-observer
  "Constructs a new `HoneyObserver` component for the provided dataset, using
  the secret writekey. Other options will be merged into the component.

  By default, this sends all event fields provided, after some formatting to
  make them compatible with Honeycomb's supported types. You can provide some
  custom preprocessing logic by setting the `:transform` key to a function
  which accepts the event data and returns an updated map of data to send. If
  the function returns nil or an empty map, the event will be discarded.

  One use of this is to set `rename-default-fields` to map ken's internal keys
  to match the default Honeycomb schema."
  [dataset writekey & {:as opts}]
  (map->HoneyObserver
    (assoc opts
           :dataset dataset
           :writekey writekey)))
