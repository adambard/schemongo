(ns schemongo.models
  (:require
    [schemongo.validators :as validators]
    [somnium.congomongo :as congo]))

; Basic getters/setters

(defn fetch-record
  "Fetch a record by id (either ObjectId or string)"
  [coll id]
  (congo/fetch-by-id coll (congo/object-id (str id))))

(defn create-record!
  "Wrap congomongo/insert!, validating <data> against <schema> first"
  [coll schema data & args]
  (let [[data err] (validators/validate-data data schema)
        result (if data (apply (partial congo/insert! coll data) args))]
    (if result
      [result nil]
      [nil err])))

(defn update-record!
  "Wrap congomongo/update!, replacing the
   existing document with an updated one after
   validating <data> against <schema>."
  [coll schema olddata newdata]
  (let [[data err] (validators/validate-data (merge olddata newdata) schema)
        newdata (merge olddata data)
        ]
    (if data
      (do
        (congo/update! coll {:_id (:_id olddata)} newdata)
        [newdata nil])
      [nil err])))

(defn destroy-record! [coll data]
  "Wrap congomongo/destroy!"
  (congo/destroy! coll data))

; Autogenerate CRUD functions for a model!

(defmacro model [name coll schema]
  `(let [fetch-fn# (symbol (str "fetch-" ~name))
         create-fn# (symbol (str "create-" ~name "!"))
         update-fn# (symbol (str "update-" ~name "!"))
         delete-fn# (symbol (str "delete-" ~name "!"))
         ]
     ; Fetch
     (intern *ns* fetch-fn#
             (fn [id# & args#] (fetch-record ~coll id#)))

     ; Create
     (intern *ns* create-fn#
             (fn [data#] (create-record! ~coll ~schema data#)))

     ; Update
     (intern *ns* update-fn#
             (fn [obj# data#] (update-record! ~coll ~schema obj# data#)))

     ; Delete
     (intern *ns* delete-fn#
             (fn [obj#] (destroy-record! ~coll obj#)))
     ))
