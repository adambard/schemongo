(ns schemongo.validators
  (:require
    [schemongo.error :as error]
    [somnium.congomongo :as congo])
  (:import org.bson.types.ObjectId))

; Helpers
(defn- notnil? [x] (not (nil? x)))
(defn- all [coll]
  (boolean (reduce #(and %1 %2) coll)))

(defn- optional
  "Accept a validator function and make it pass for nulls too"
  [vldtr]
  (fn [datum]
      (or (nil? datum) (vldtr datum))))

; Schema validation

(declare validate-data)

; Basic primitives
(def validate-str string?)

(defn validate-int [datum]
  (or (instance? Integer datum)
      (instance? Long datum)))

(def validate-float (partial instance? Double))
(def validate-bool (partial instance? Boolean))

; More complex validators
(defn validate-foreign
  "Ensure that collection <coll> exists matching ObjectId <datum>"
  [coll datum]
  (and
    (instance? ObjectId datum)
    (notnil? (congo/fetch-by-id coll datum))))

(defn validate-many
  "A list of foreign ObjectIds. Only false if any of them does not exist"
  [coll ids]
  (if (empty? ids)
    true ; Always true for empty collections
    (boolean (and
               (coll? ids)
               (all (map (partial validate-foreign coll) ids))))))

(defn validate-enum
  "Make sure that <datum> is one of the entries in <opts>"
  [opts datum]
  (boolean (some #{datum} opts)))

(defn validate-enum-many
  "Make sure that each entry in <datum> is one of the entries in <opts>"
  [opts data]
  (cond
    (nil? data) false
    (empty? opts) false
    (empty? data) false
    :else (all (map (partial validate-enum opts) data))))

(defn validate-enum-many?
  "Make sure that each entry in <datum> is one of the entries in <opts>, or empty"
  [opts data]
  (cond
    (empty? data) true
    :else (validate-enum-many opts data)))

(defn validate-date
  "Make sure d is a date tuple in the form [yyyy mm dd]"
  [d]
  (and
    (coll? d)
    (= (count d) 3)
    (validate-int (nth d 0))
    (validate-int (nth d 1))
    (validate-int (nth d 2))
    (and (> (nth d 0) 1900) (< (nth d 0) 2100))
    (and (>= (nth d 1) 1) (<= (nth d 1) 12))
    (and (>= (nth d 2) 1) (<= (nth d 2) 31))
    true
    ))

(defn validate-time
  "Make sure <t> is a time tuple in the form [HH MM SS], with integers"
  [t]
  (and
    (coll? t)
    (= (count t) 3)
    (validate-int (nth t 0))
    (validate-int (nth t 1))
    (validate-int (nth t 2))
    (and (>= (nth t 0) 0) (< (nth t 0) 24))
    (and (>= (nth t 1) 1) (< (nth t 1) 60))
    (and (>= (nth t 2) 0) (< (nth t 2) 60))
    ))

(defn validate-custom [datum f & args]
  (apply f datum args))

(defn validate-datetime
  "Make sure d is a datetime tuple in the form [yyyy mm dd HH MM SS]"
  [d]
  (and
    (coll? d)
    (= (count d) 6)
    (validate-date (take 3 d))
    (validate-time (take-last 3 d))))

(def validators {
     ; Simple types
     :str validate-str
     :str? (optional validate-str)
     :int validate-int
     :int? (optional validate-int)
     :float validate-float
     :float? (optional validate-float)
     :bool validate-bool
     :bool? (optional validate-bool)

     ; Relations
     :foreign validate-foreign
     :foreign? (fn [coll datum] (or (nil? datum) (validate-foreign coll datum)))
     :many validate-many
     :many? validate-many ; Nullability is implied

     ; Date and time
     :date validate-date
     :date? (optional validate-date)
     :datetime validate-datetime
     :datetime? (optional validate-datetime) 
     :time validate-time
     :time? (optional validate-time)

     ; Collections
     :enum validate-enum
     :enum-many validate-enum-many
     :enum-many? validate-enum-many?

     ; Do-it-yourself
     :custom validate-custom

     ; Embedded documents
     :embed (fn [datum schema defaults]
                (let [[d err] (validate-data (merge datum defaults) schema)]
                  (nil? err)))
     })

; Might want to extend this later
(def is-valid? boolean)

(defn validate-field
  "Validate <field> in map <datum> against the validator for <type>"
  [datum field type & args]
  (let [val-fn (validators type)
        valid? (cond
                 ; I know this could be pared down, but for easier understanding
                 ; each of these is separate so they can be commented.

                 (= type :custom) (is-valid? (apply val-fn datum args))

                 (= type :embed) (is-valid? (val-fn datum (first args) (nth args 1)))
                 ; Foreign key - args is a collection
                 (= type :foreign) (is-valid? (val-fn (first args) datum))
                 (= type :foreign?) (is-valid? (val-fn (first args) datum))
                 (= type :many) (is-valid? (val-fn (first args) datum))

                 ; List - args is a type to validate each item by
                 (= type :list) (is-valid? (val-fn (first args) datum))

                 ; Enum - args is the list of allowable values
                 (= type :enum) (is-valid? (val-fn (first args) datum))
                 (= type :enum-many) (is-valid? (val-fn (first args) datum))

                 ; The args - Just apply the validator to the data
                 :else (is-valid? (val-fn datum))

                 )]
    (if valid?
      [true nil]
      [nil (str "Field " field " was invalid.")])))

(defn validate-data
  "Validate <data> against <schema>."
  [data schema]
  (let [data (select-keys data (for [[field & tail] schema] field))]
    (loop [schm (first schema) left (rest schema)]
      (let [[field type & rst] schm
            datum (field data)
            args (error/unit-error (vec (concat [datum field type] rst)))
            mval (error/bind-error args validate-field)
            ]
        (cond
          (not (nil? (last mval))) mval ; Short-circuit for errors
          (not (empty? left)) (recur (first left) (rest left))
          :else [data nil] )))))
