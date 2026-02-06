(ns ken.honeycomb-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ken.honeycomb :as hc])
  (:import
    (io.honeycomb.libhoney
      Event
      HoneyClient
      LibHoney)
    (io.honeycomb.libhoney.responses
      ResponseObservable)
    (io.honeycomb.libhoney.transport
      Transport)))


(deftest event-processing
  (testing "filtering"
    (is (nil? (#'hc/format-fields
               identity
               {})))
    (is (nil? (#'hc/format-fields
               (constantly nil)
               {:foo 123})))
    (is (nil? (#'hc/format-fields
               #(dissoc % :foo)
               {:foo 123})))
    (is (= {"bar" true}
           (#'hc/format-fields
            #(dissoc % :foo)
            {:foo 123
             :bar true}))))
  (testing "primitive values"
    (is (= {"nil" nil
            "bool" true
            "char" "c"
            "str" "string"
            "int" 123
            "double" 3.14
            "ratio" 0.4
            "sym" "symbolic"}
           (#'hc/format-fields
            identity
            {:nil nil
             :bool true
             :char \c
             :str "string"
             :int 123
             :double 3.14
             :ratio 4/10
             :sym 'symbolic
             :ken.event/time (java.time.Instant/parse "1999-12-31T08:00:00.000Z")
             :ken.event/sample-rate 10
             :ken.trace/upstream-sample-rate 10}))))
  (testing "honeycomb event fields"
    (is (= {"foo" "bar"}
           (#'hc/format-fields
            identity
            {:foo "bar"
             :ken.event/time (java.time.Instant/parse "1999-12-31T08:00:00.000Z")
             :ken.event/sample-rate 10
             :ken.trace/upstream-sample-rate 10}))
        "should not be included in field data"))
  (testing "collections"
    (is (= {"set" #{1 2 3}
            "list" '("a")
            "vec" ["x" "y" "z"]}
           (#'hc/format-fields
            identity
            {:set #{1 2 3}
             :list '("a")
             :vec [:x :y :z]})))
    (is (= {"too-long" (range 1000)}
           (#'hc/format-fields
            identity
            {:too-long (range)}))
        "infinite sequences should be truncated"))
  (testing "throwables"
    (is (= {"error" {"class" "java.lang.IllegalArgumentException"
                     "message" "Uh oh, you did the thing wrong"}}
           (#'hc/format-fields
            identity
            {:error (IllegalArgumentException.
                      "Uh oh, you did the thing wrong")})))
    (is (= {"error" {"class" "clojure.lang.ExceptionInfo"
                     "message" "An error with some data and a cause"
                     "data" {"foo" 123
                             "bar" true}
                     "cause" {"class" "java.lang.RuntimeException"
                              "message" "BOOM"}}}
           (#'hc/format-fields
            identity
            {:error (ex-info "An error with some data and a cause"
                             {:foo 123
                              :bar true}
                             (RuntimeException. "BOOM"))})))))


(defn- get-mock-honeyclient
  "Mock honeyclient based on https://github.com/honeycombio/libhoney-java/blob/main/libhoney/src/main/java/io/honeycomb/libhoney/HoneyClient.java"
  []
  (let [mock-observable (proxy [ResponseObservable] []
                          (add [observer] nil)

                          (remove [observer] nil)

                          (publish [response] nil))
        mock-transport (reify Transport
                         (submit [_this _event] true)

                         (close [_this] nil)

                         (getResponseObservable [_this] mock-observable))
        options (-> (LibHoney/options)
                    (.setWriteKey "test-key")
                    (.setDataset "test-dataset")
                    (.build))]
    (HoneyClient. options mock-transport)))


(defn- get-event-data
  [^Event event]
  {:sample-rate (.getSampleRate event)
   :timestamp (.getTimestamp event)
   :fields (.getFields event)})


(deftest create-event-test
  (let [mock-time (java.time.Instant/parse "1999-12-31T08:00:00.000Z")
        honeyclient (get-mock-honeyclient)]
    (testing "create-event fields"
      (is (= {"foo" "bar"}
             (-> (#'hc/create-event honeyclient identity {:foo "bar"
                                                          :ken.event/time mock-time})
                 (.getFields)))
          "should be stored in epoch format"))
    (testing "create-event timestamp"
      (is (= (inst-ms mock-time)
             (-> (#'hc/create-event honeyclient identity {:foo "bar"
                                                          :ken.event/time mock-time})
                 (.getTimestamp)))
          "should be stored in epoch format"))
    (testing "create-event sample-rate"
      (is (= {:timestamp (inst-ms mock-time)
              :sample-rate 1
              :fields {"foo" "bar"}}
             (-> (#'hc/create-event honeyclient identity {:foo "bar"
                                                          :ken.event/time mock-time})
                 (get-event-data)))
          "should be 1 if not provided")
      (is (= {:timestamp (inst-ms mock-time)
              :sample-rate 10
              :fields {"foo" "bar"}}
             (-> (#'hc/create-event honeyclient identity {:foo "bar"
                                                          :ken.event/time mock-time
                                                          :ken.event/sample-rate 10})
                 (get-event-data)))
          "should be set by :ken.event/sample-rate when provided")
      (is (= {:timestamp (inst-ms mock-time)
              :sample-rate 100
              :fields {"foo" "bar"}}
             (-> (#'hc/create-event honeyclient identity {:foo "bar"
                                                          :ken.event/time mock-time
                                                          :ken.trace/upstream-sample-rate 100})
                 (get-event-data)))
          "should be set by :ken.event/sample-rate when provided")
      (is (= {:timestamp (inst-ms mock-time)
              :sample-rate 10
              :fields {"foo" "bar"}}
             (-> (#'hc/create-event honeyclient identity {:foo "bar"
                                                          :ken.event/time mock-time
                                                          :ken.event/sample-rate 10
                                                          :ken.trace/upstream-sample-rate 100})
                 (get-event-data)))
          "should prefer :ken.event/sample-rate if both provided (should only occur if user manipulates data by hand)"))
    (.close honeyclient)))
