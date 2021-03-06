(ns ken.honeycomb-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ken.honeycomb :as hc]))


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
             :sym 'symbolic}))))
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
