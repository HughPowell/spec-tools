(ns spec-tools.swagger.core-test
  (:require
    [clojure.test :refer [deftest testing is are]]
    [spec-tools.swagger.core :as swagger]
    [clojure.spec.alpha :as s]
    [spec-tools.spec :as spec]
    #?(:clj [ring.swagger.validator :as v])
    [spec-tools.core :as st]))

(s/def ::integer integer?)
(s/def ::string string?)
(s/def ::set #{1 2 3})
(s/def ::keys (s/keys :req-un [::integer]))
(s/def ::spec (st/spec
                {:spec string?
                 :description "description"
                 :json-schema/default "123"
                 :json-schema/example "json-schema-example"
                 :swagger/example "swagger-example"}))
(s/def ::keys2 (s/keys :req-un [::integer ::spec]))

(def exceptations
  {int?
   {:type "integer", :format "int64"}

   integer?
   {:type "integer"}

   float?
   {:type "number" :format "float"}

   double?
   {:type "number" :format "double"}

   string?
   {:type "string"}

   boolean?
   {:type "boolean"}

   nil?
   {}

   #{1 2 3}
   {:enum [1 3 2], :type "string"}

   (s/int-in 1 10)
   {:type "integer"
    :format "int64"
    :x-allOf [{:type "integer"
               :format "int64"}
              {:minimum 1
               :maximum 10}]}

   (s/keys :req-un [::integer] :opt-un [::string])
   {:type "object"
    :properties {"integer" {:type "integer"}
                 "string" {:type "string"}}
    :required ["integer"]}

   ::keys
   {:type "object",
    :properties {"integer" {:type "integer"}},
    :required ["integer"],
    :title "spec-tools.swagger.core-test/keys"}

   (s/and int? pos?)
   {:type "integer"
    :format "int64",
    :x-allOf [{:type "integer"
               :format "int64"}
              {:minimum 0
               :exclusiveMinimum true}]}

   (s/and spec/int?)
   {:type "integer"
    :format "int64",
    :x-allOf [{:type "integer"
               :format "int64"}]}

   (s/or :int int? :pos pos?)
   {:type "integer"
    :format "int64",
    :x-anyOf [{:type "integer"
               :format "int64"}
              {:minimum 0
               :exclusiveMinimum true}]}

   (s/merge (s/keys :req-un [::integer])
            (s/keys :req-un [::string]))
   {:type "object",
    :properties {"integer" {:type "integer"},
                 "string" {:type "string"}},
    :required ["integer" "string"]}

   (st/merge (s/keys :req-un [::integer])
             (s/keys :req-un [::string]))
   {:type "object",
    :properties {"integer" {:type "integer"},
                 "string" {:type "string"}},
    :required ["integer" "string"]}

   (s/merge (s/keys :req-un [::integer])
            (s/or :foo (s/keys :req-un [::string])
                  :bar (s/keys :req-un [::set])))
   {:type "object",
    :properties {"integer" {:type "integer"},
                 "string" {:type "string"}
                 "set" {:enum [1 3 2]
                        :type "string"}},
    :required ["integer"]}

   sequential?
   {:type "array" :items {}}

   (s/every integer?)
   {:type "array", :items {:type "integer"}}

   (s/every-kv string? integer?)
   {:type "object", :additionalProperties {:type "integer"}}

   (s/coll-of string?)
   {:type "array", :items {:type "string"}}

   (s/coll-of string? :into '())
   {:type "array", :items {:type "string"}}

   (s/coll-of string? :into [])
   {:type "array", :items {:type "string"}}

   (s/coll-of string? :into #{})
   {:type "array", :items {:type "string"}, :uniqueItems true}

   (s/map-of string? integer?)
   {:type "object", :additionalProperties {:type "integer"}}

   (s/* integer?)
   {:type "array", :items {:type "integer"}}

   (s/+ integer?)
   {:type "array", :items {:type "integer"}, :minItems 1}

   (s/? integer?)
   {:type "array", :items {:type "integer"}, :minItems 0}

   (s/alt :int integer? :string string?)
   {:type "integer", :x-anyOf [{:type "integer"} {:type "string"}]}

   (s/cat :int integer? :string string?)
   {:type "array"
    :items {:type "integer"
            :x-anyOf [{:type "integer"}
                      {:type "string"}]}}

   (s/tuple integer? string?)
   {:type "array"
    :items {}
    :x-items [{:type "integer"} {:type "string"}]}

   (s/map-of string? clojure.core/integer?)
   {:type "object", :additionalProperties {:type "integer"}}

   (s/nilable string?)
   {:type "string", :x-nullable true}

   ::spec
   {:type "string"
    :description "description"
    :default "123"
    :title "spec-tools.swagger.core-test/spec"
    :example "swagger-example"}})

(deftest test-expectations
  (doseq [[spec swagger-spec] exceptations]
    (is (= swagger-spec (swagger/transform spec)))))

(s/def ::ref-spec (st/spec
                    {:spec          ::keys2
                     :description   "description"
                     :swagger/title "RefSpec"}))

(s/def ::coll-ref-spec (st/spec
                         {:spec (s/coll-of ::ref-spec)}))
(def ref-expectations
  (merge
    exceptations
    {::keys
     {:$ref "#/definitions/spec-tools.swagger.core-test.keys"
      ::swagger/definitions {"spec-tools.swagger.core-test.keys"
                             {:type "object",
                              :properties {"integer" {:type "integer"}},
                              :required ["integer"]}}}

     ::ref-spec
     {:$ref "#/definitions/RefSpec"
      ::swagger/definitions {"RefSpec" {:type "object"
                                        :properties  {"integer" {:type "integer"}
                                                      "spec" {:type "string"
                                                              :description "description"
                                                              :title "spec-tools.swagger.core-test/spec"
                                                              :default "123"
                                                              :example "swagger-example"}}
                                        :required ["integer" "spec"]
                                        :description "description"}}}

     (s/keys :req [::ref-spec])
     {:type "object"
      :properties {"spec-tools.swagger.core-test/ref-spec" {:$ref "#/definitions/RefSpec"}}
      :required ["spec-tools.swagger.core-test/ref-spec"]
      ::swagger/definitions {"RefSpec" {:type "object"
                                        :properties {"integer" {:type "integer"}
                                                     "spec" {:type "string"
                                                             :description "description"
                                                             :title "spec-tools.swagger.core-test/spec"
                                                             :default "123"
                                                             :example "swagger-example"}}
                                        :required ["integer" "spec"]
                                        :description "description"}}}

     ::coll-ref-spec
     {:type "array"
      :items {:$ref "#/definitions/RefSpec"}
      ::swagger/definitions {"RefSpec" {:type "object"
                                        :properties {"integer" {:type "integer"}
                                                     "spec" {:type "string"
                                                             :description "description"
                                                             :title "spec-tools.swagger.core-test/spec"
                                                             :default "123"
                                                             :example "swagger-example"}}
                                        :required ["integer" "spec"]
                                        :description "description"}}
      :title "spec-tools.swagger.core-test/coll-ref-spec"}}))

(deftest test-expectations-with-refs
  (doseq [[spec swagger-spec] ref-expectations]
    (is (= swagger-spec (swagger/transform spec {:refs? true :type :schema})))
    (is (= swagger-spec (swagger/transform spec {:refs? true :in :body})))))

(s/def ::default-titled-string (st/create-spec {:spec ::string}))
(s/def ::explicitly-titled-string (st/create-spec {:spec          ::string
                                                   :swagger/title "String Title"}))
(deftest test-expectations-with-default-titles
  (testing "automatically generated title is included by default"
    (is ::default-titled-string (keyword (:title (swagger/transform ::default-titled-string)))))
  (testing "explicitly exclude default title"
    (is (not (contains? (swagger/transform ::default-titled-string {:default-titles? false}) :title))))
  (testing "Swagger title is included irrespective"
    (is (= "String Title" (:title (swagger/transform ::explicitly-titled-string {:default-titles? false}))))))

(deftest parameter-test
  (testing "nilable body is not required"
    (is (= [{:in "body",
             :name "body",
             :description "",
             :required false,
             :schema {:type "object",
                      :title "spec-tools.swagger.core-test/keys2",
                      :properties {"integer" {:type "integer"}
                                   "spec" {:default "123"
                                           :description "description"
                                           :example "swagger-example"
                                           :title "spec-tools.swagger.core-test/spec"
                                           :type "string"}},
                      :required ["integer" "spec"],
                      :x-nullable true}}]
           (swagger/extract-parameter :body (s/nilable ::keys2)))))

  (testing "definitions are raised to the top of the parameter"
    (is (=
          [{:in "body"
            :name "spec-tools.swagger.core-test/ref-spec"
            :description ""
            :required true
            :schema {:$ref "#/definitions/RefSpec"
                     ::swagger/definitions {"RefSpec" {:type "object"
                                                       :properties {"integer" {:type "integer"}
                                                                    "spec" {:type "string"
                                                                            :description "description"
                                                                            :title "spec-tools.swagger.core-test/spec"
                                                                            :default "123"
                                                                            :example "swagger-example"}}
                                                       :required ["integer" "spec"]
                                                       :description "description"}}}}]
          (swagger/extract-parameter :body ::ref-spec {:refs? true})))))

#?(:clj
   (deftest test-parameter-validation
     (let [swagger-spec (fn [schema]
                          {:swagger "2.0"
                           :info {:title "" :version ""}
                           :paths {"/hello" {:get
                                             {:responses
                                              {200 {:description ""
                                                    :schema schema}}}}}})]

       (testing "invalid schema fails on swagger spec validation"
         (is (-> {:type "invalid"} swagger-spec v/validate)))

       (testing "all expectations pass the swagger spec validation"
         (doseq [[spec] exceptations]
           (is (= nil (-> spec swagger/transform swagger-spec v/validate)))
           (is (nil? (-> spec (swagger/transform {:refs? true}) swagger-spec v/validate))))))))

(s/def ::id string?)
(s/def ::name string?)
(s/def ::street string?)
(s/def ::city (s/nilable #{:tre :hki}))
(s/def ::address (s/keys :req-un [::street ::city]))
(s/def ::user (s/keys :req-un [::id ::name ::address]))

(deftest expand-test

  (testing "::parameters"
    (is (= {:parameters [{:in "query"
                          :name "name2"
                          :description "this survives the merge"
                          :type "string"
                          :required true}
                         {:in "query"
                          :name "name"
                          :description ""
                          :type "string"
                          :required false}
                         {:in "query"
                          :name "street"
                          :description ""
                          :type "string"
                          :required false}
                         {:in "query"
                          :name "city"
                          :description ""
                          :type "string"
                          :required false
                          :enum [:tre :hki]
                          :allowEmptyValue true}
                         {:in "path"
                          :name "spec-tools.swagger.core-test/id"
                          :description ""
                          :type "string"
                          :required true}
                         {:in "body",
                          :name "spec-tools.swagger.core-test/address",
                          :description "",
                          :required true,
                          :schema {:type "object",
                                   :title "spec-tools.swagger.core-test/address",
                                   :properties {"street" {:type "string"},
                                                "city" {:enum [:tre :hki],
                                                        :type "string"
                                                        :x-nullable true}},
                                   :required ["street" "city"]}}]}
           (swagger/swagger-spec
             {:parameters [{:in "query"
                            :name "name"
                            :description "this will be overridden"
                            :required false}
                           {:in "query"
                            :name "name2"
                            :description "this survives the merge"
                            :type "string"
                            :required true}]
              ::swagger/parameters
              {:query (s/keys :opt-un [::name ::street ::city])
               :path (s/keys :req [::id])
               :body ::address}})))
    (is (= {:parameters [{:in "query"
                          :name "name2"
                          :description "this survives the merge"
                          :type "string"
                          :required true}
                         {:in "query"
                          :name "name"
                          :description ""
                          :type "string"
                          :required false}
                         {:in "query"
                          :name "street"
                          :description ""
                          :type "string"
                          :required false}
                         {:in "query"
                          :name "city"
                          :description ""
                          :type "string"
                          :required false
                          :enum [:tre :hki]
                          :allowEmptyValue true}
                         {:in "path"
                          :name "spec-tools.swagger.core-test/id"
                          :description ""
                          :type "string"
                          :required true}
                         {:in "body",
                          :name "spec-tools.swagger.core-test/address",
                          :description "",
                          :required true,
                          :schema {:type "object",
                                   :title "spec-tools.swagger.core-test/address",
                                   :properties {"street" {:type "string"},
                                                "city" {:enum [:tre :hki],
                                                        :type "string"
                                                        :x-nullable true}},
                                   :required ["street" "city"]}}]}
           (swagger/swagger-spec
             {:parameters [{:in "query"
                            :name "name"
                            :description "this will be overridden"
                            :required false}
                           {:in "query"
                            :name "name2"
                            :description "this survives the merge"
                            :type "string"
                            :required true}]
              ::swagger/parameters
              {:query (st/create-spec {:spec (s/keys :opt-un [::name ::street ::city])})
               :path (st/create-spec {:spec (s/keys :req [::id])})
               :body (st/create-spec {:spec ::address})}}))))

  (testing "::parameters with refs"
    (is (=
          {:parameters [{:in "body",
                         :name "spec-tools.swagger.core-test/ref-spec",
                         :description "",
                         :required true,
                         :schema {:$ref "#/definitions/RefSpec"}}],
           :definitions {"RefSpec" {:type "object",
                                    :properties {"integer" {:type "integer"},
                                                 "spec" {:type "string",
                                                         :description "description",
                                                         :title "spec-tools.swagger.core-test/spec",
                                                         :default "123",
                                                         :example "swagger-example"}},
                                    :required ["integer" "spec"],
                                    :description "description"}}}
          (swagger/swagger-spec
            {::swagger/parameters {:body ::ref-spec}}
            {:refs? true}))))

  (testing "::responses"
    (is (= {:responses
            {200 {:schema
                  {:type "object"
                   :properties
                   {"id" {:type "string"}
                    "name" {:type "string"}
                    "address" {:type "object"
                               :properties {"street" {:type "string"}
                                            "city" {:enum [:tre :hki]
                                                    :type "string"
                                                    :x-nullable true}}
                               :required ["street" "city"]
                               :title "spec-tools.swagger.core-test/address"}}
                   :required ["id" "name" "address"]
                   :title "spec-tools.swagger.core-test/user"}
                  :description ""}
             404 {:description "Ohnoes."}
             500 {:description "fail"}}}
           (swagger/swagger-spec
             {:responses {404 {:description "fail"}
                          500 {:description "fail"}}
              ::swagger/responses {200 {:schema ::user}
                                   404 {:description "Ohnoes."}}}))))

  (testing "::responses with refs"
    (is (=
          {:responses
           {200 {:schema
                 {:$ref "#/definitions/User"},
                 :description ""}},
           :definitions {"User"
                         {:type "object",
                          :properties {"id" {:type "string"},
                                       "name" {:type "string"},
                                       "address" {:$ref "#/definitions/spec-tools.swagger.core-test.address"}},
                          :required ["id" "name" "address"]}
                         "spec-tools.swagger.core-test.address"
                         {:type "object",
                          :properties {"street" {:type "string"},
                                       "city" {:enum [:tre :hki],
                                               :type "string",
                                               :x-nullable true}},
                          :required ["street" "city"]}}}
          (swagger/swagger-spec
            {::swagger/responses {200 {:schema (st/create-spec
                                                 {:spec ::user
                                                  :swagger/title "User"})}}}
            {:refs? true}))))

  (testing "::responses with refs in additionalProperties"
    (is (=
          {:responses {200 {:schema {:$ref "#/definitions/Every Test"}, :description ""}},
           :definitions {"Every Test" {:type "object",
                                       :additionalProperties {:$ref "#/definitions/spec-tools.swagger.core-test.address"}},
                         "spec-tools.swagger.core-test.address" {:type "object",
                                                                 :properties {"street" {:type "string"},
                                                                              "city" {:enum [:tre :hki],
                                                                                      :type "string",
                                                                                      :x-nullable true}},
                                                                 :required ["street" "city"]}}}
          (swagger/swagger-spec
            {::swagger/responses {200 {:schema (st/create-spec
                                                 {:spec (s/every-kv ::id ::address)
                                                  :swagger/title "Every Test"})}}}
            {:refs? true})))))

#?(:clj
   (deftest test-schema-validation
     (let [data {:swagger "2.0"
                 :info {:version "1.0.0"
                        :title "Sausages"
                        :description "Sausage description"
                        :termsOfService "http://helloreverb.com/terms/"
                        :contact {:name "My API Team"
                                  :email "foo@example.com"
                                  :url "http://www.metosin.fi"}
                        :license {:name "Eclipse Public License"
                                  :url "http://www.eclipse.org/legal/epl-v10.html"}}
                 :tags [{:name "user"
                         :description "User stuff"}]
                 :paths {"/api/ping" {:get {:responses {:default {:description ""}}}}
                         "/user/:id" {:post {:summary "User Api"
                                             :description "User Api description"
                                             :tags ["user"]
                                             ::swagger/parameters {:path (s/keys :req [::id])
                                                                   :body ::user}
                                             ::swagger/responses {200 {:schema ::user
                                                                       :description "Found it!"}
                                                                  404 {:description "Ohnoes."}}}}}}]
       (is (nil? (-> data swagger/swagger-spec v/validate)))
       (is (nil? (-> data (swagger/swagger-spec {:refs? true}) v/validate))))))


(deftest backport-swagger-meta-unnamespaced
  (is (= (swagger/transform
           (st/spec {:spec string?
                     :swagger {:type "string"
                               :format "password"
                               :random-value "42"}}))
         {:type "string" :format "password" :random-value "42"}))

  (is (= (swagger/transform
           (st/spec {:spec string?
                     :swagger {:type "object"}
                     :swagger/format "password"}))
         {:type "object"}))

  (is (= (swagger/transform
           (st/spec {:spec string?
                     :swagger/type "string"
                     :swagger/format "password"
                     :swagger/random-value "42"}))
         {:type "string" :format "password" :random-value "42"})))
