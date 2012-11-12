(ns schemongo.test.models
  (:require
    [schemongo.models :as models]
    [schemongo.conn :as conn]
    [somnium.congomongo :as congo])
  (:use [clojure.test]))

; Test data
(def schema [
  [:str_field :str]
  [:int_field :int]
  [:float_field :float]
  [:enum_field :enum ["a" "b" "c"]]
  [:enum_many_field :enum-many ["a" "b" "c"]]
  [:date_field :date]
])

(def defaults {:str_field "Test"
               :int_field 1
               :float_field 1.0
               :enum_field "a"
               :enum_many_field ["a" "b"]
               :date_field [2010 1 1]})

(models/model "testobj" :testobjs schema)

(defn model-test-fixture [f]
  ; Setup: Connect to mongo db
  (conn/connect-uri! "mongodb://127.0.0.1:27017/schemongo-test")

  ; Run the tests
  (f)

  ; Teardown: Delete all records in the :testobjs collection
  (congo/destroy! :testobjs {}))

(use-fixtures :each model-test-fixture)


; TESTS

(deftest test-create-failures

  ; Not enough info
  (let [
    [obj err] (create-testobj! {:str_field "Test"})]
    (is (nil? obj))))

(deftest test-enum-many
  ; Empty field
  (let [
    [obj err] (create-testobj! (merge defaults {:enum_many_field ["f"]}))]
    (is (nil? obj))))

(deftest test-enum
  ; Enumeration
  (let [
    [obj err] (create-testobj! (merge defaults {:enum_field "d"}))]
    (is (nil? obj))))

(deftest test-create-pretty-much-works
  ; Make sure the object is created
  (let [obj (congo/fetch-one :testobjs)]
    (is (nil? obj)))

  (let [
        [obj err] (create-testobj! defaults)
        obj (congo/fetch-one :testobjs)]
      (is (not (nil? obj)))
      (is (= (:enum_field obj) "a"))))

(deftest test-update-works
  (let [[obj err] (create-testobj! defaults)
        [obj err] (update-testobj! obj {:str_field "teststr"})
        obj (fetch-testobj (:_id obj))
        ]
    (is (= (:str_field obj) "teststr"))))

(deftest test-delete
    (let [[obj err] (create-testobj! defaults)]
      (is (not (nil? obj))))

    (let [obj (congo/fetch-one :testobjs)]
      (is (not (nil? obj)))
      (delete-testobj! obj))

    (let [obj (congo/fetch-one :testobjs)]
      (is (nil? obj)))  )
