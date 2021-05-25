(ns ken.honeycomb-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ken.honeycomb :as hc]))


(deftest event-processing
  (testing "primitive values"
    (is (= {"nil" nil
            "bool" true
            "char" "c"
            "str" "string"
            "int" 123
            "double" 3.14
            "ratio" 0.4
            "sym" "symbolic"}
           (#'hc/process-event
            {}
            {:nil nil
             :bool true
             :char \c
             :str "string"
             :int 123
             :double 3.14
             :ratio 4/10
             :sym 'symbolic}))))
  (testing "collections"
    (is (= {"set" #{1 2 3}
            "list" '("a")
            "vec" ["x" "y" "z"]}
           (#'hc/process-event
            {}
            {:set #{1 2 3}
             :list '("a")
             :vec [:x :y :z]})))
    (is (= {"too-long" (range 1000)}
           (#'hc/process-event
            {}
            {:too-long (range)}))
        "infinite sequences should be truncated"))
  (testing "throwables"
    (is (= {"error" {"class" "java.lang.IllegalArgumentException"
                     "message" "Uh oh, you did the thing wrong"}}
           (#'hc/process-event
            {}
            {:error (IllegalArgumentException.
                      "Uh oh, you did the thing wrong")})))
    (is (= {"error" {"class" "clojure.lang.ExceptionInfo"
                     "message" "An error with some data and a cause"
                     "data" {"foo" 123
                             "bar" true}
                     "cause" {"class" "java.lang.RuntimeException"
                              "message" "BOOM"}}}
           (#'hc/process-event
            {}
            {:error (ex-info "An error with some data and a cause"
                             {:foo 123
                              :bar true}
                             (RuntimeException. "BOOM"))})))))
