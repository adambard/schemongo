(ns schemongo.test.validators
  (:require
    [schemongo.validators :as validators]
    [schemongo.models :as models]
    [schemongo.conn :as conn])
  (:use [clojure.test]))

(deftest test-str-validator
  (is (false? ((validators/validators :str) 1)))
  (is (false? ((validators/validators :str) ["Asdf"])))
  (is (false? ((validators/validators :str) #(print %))))
  (is (true? ((validators/validators :str) "Hello"))))

(deftest test-int-validator
         (is (false? ((validators/validators :int) "1")))
         (is (true? ((validators/validators :int) 1))))

(deftest test-date-validator
    (let [f (validators/validators :date)]
      (is (false? (f [])))
      (is (false? (f [2000])))
      (is (false? (f [2000 1])))
      (is (false? (f "2000-01-02")))
      (is (true? (f [2000 1 2])))))

(deftest test-enum-validator
         (let [f (partial (validators/validators :enum) ["a" "b" "c"])]
           (is (false? (f "x")))
           (is (false? (f 1)))
           (is (false? (f #(print %))))
           (is (false? (f 1.0)))
           (is (false? (f ["a"])))
           (is (true? (f "a")))
           (is (true? (f "b")))
           (is (true? (f "c")))
           (is (false? (f "d")))
           ))


(deftest test-foreign-validator
         (conn/connect-uri! "mongodb://127.0.0.1:27017/validator-test")
         (let [f (partial (validators/validators :foreign) :users)
               [u err] (models/create-record! :users [[:username :str]] {:username "test"})
               ]
           (is (false? (f nil)))
           (is (false? (f "asdfasd9fasdfasd")))
           (is (true? (f (:_id u))))
           ))
