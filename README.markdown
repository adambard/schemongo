Schemongo
=============

**Latest Version:** 0.1.2

Practice safe mongo! A mongo schema library with two features of note:

1. Validators for field types, including enumerable field types and BSON ObjectIds, and
2. A model macro and functions to shortcut the validation and creation/updating/deletion of data

Installation
=============

Include in your lein :dependencies

    [schemongo "0.1.2"]

This will resolve the dependencies on congomongo and clojure 1.3.0

Quickstart
===============

    (ns quickstart
      (:require [schemongo.models :as models]))

    ; Declare a schema like this:

    (def user-schema [
      [:username :str]
      [:passwordhash :str]
      [:salt :str]])

    ; Then, create CRUD functions using model:

    (model "user" :users user-schema)

Now you can use these functions:

    (fetch-user objectid)
    (create-user! data)
    (update-user! user data)
    (delete-user! user)

For create and update, the output is an error tuple (the error is skipped for fetch, nil is just returned).
Here's a contrived example:

    (let [
          [usr err] (create-user! {:username "Username"
                                   :passwordhash "No functional validations, just types"
                                   :salt "Some salt"})]
           ; Check that usr is not nil, or that err is.

           (if (nil? err)
             (update-user! usr {:username "Otherusername"}))
    )


schemongo.models
==================

If you use only one part of this library, use this one (because it provides validations implicitly)

**(model name coll schema)**

Creates CRUD functions for a model named *name*, stored in *coll* and validated against *schema*

    (model "user" :users user-schema)

schemongo.validators
=====================

This module contains and exports validation functions for all supported types, and
packaged validation of data (as a hash-map) against a defined schema.

A schema is a vector of entries in the following format: [name type & args]. Examples:

    [[:intval :int]
     [:strval :str]
     [:enumval :enum ["a" "b" "c"]]
     [:user :foreign :users]]

**(validate-field value field type)**  
**(validate-field value field type & args)**

Validate *value* for *field* as a *type*, with *args* as arguments to the validator.
Returns [true nil] if successful and [nil *error message*] if not.

**(validate-data data schema)**

Validate all fields of *data* against *schema*. Strips any fields not in *schema* from *data*.
Returns [true nil] if successful and [nil *error message*] if not.

Will fail on the first invalid field.

Validators included:
=======================

Basic Types
------------

**:str, :int, :float, :bool**

    [:strval :str]

Assert that the type of the given data is present, and as given.

**:str?, :int?, :float?, :bool?**

    [:bool_or_nil_val :bool?]

Same as above, but will accept nil/unset values for these fields

Enumerations
----------------

**:enum, :enum?**

    (def members ["a", "b", "c"])

    ; Example schema
    [:enumfield :enum members]

Ensure that the given data is a member. :enum? will also accept an empty value.

**:enum-many, :enum-many?**

    [:enum_many_field :enum-many members]

Ensure the input is a collection, with each element present in the member vector.
**:enum-many?** will also accept an empty value.

Dates
------

This is a bit weird, don't rely on it. Use a proper date type, this is just for storage purposes.

Dates and times as vectors.

**:date, :date?**

Ensure that the given data is a list of three integers representing year, month, day.
Year is in range 1900-2100, Month is in range 1-12 inclusive, Day is in range 1-31 inclusive.
:date? will accept a nil as well.

Does not challenge putting too many days in a month, so don't rely on it.

**:time, :time?**

Ensure that the given data is a list of three integers representing hour, minute, second,
checking for the obvious ranges of 0-23, 0-59, 0-59 respectively.
:time? will accept a nil/unset as well.

Relationships
--------------

**:foreign, :foreign?**

    (def coll :users)

    ; Example schema entry
    [:owner :foreign :users]

:foreign will accept an ObjectId referring to an entry in another (or the same)
collection, and **perform a query** to ensure that it exists.
:foreign? will accept a nil as well.

**:many, :many?**

    [:owners :many :users]

Will accept a collection of ObjectIds and verify that they exist. Both are actually the
same, this time, and will return true for empty/nils.

Custom Validators
------------------

**:custom**

    (defn validate-int-in-range [d n1 n2]
     (and
       (validators/validate-int n1)
       (validators/validate-int n2)
       (validators/validate-int d)
       (>= d n1)
       (<= d n2)))

    [:number_from_one_to_ten :custom validate-int-in-range 1 10]

Give it a function, and any extra arguments to pass to the function, and it will ensure that function
returns true before writing to the database.

Changelog
=========

**0.1.2** - Initial public release

License
========

Licensed under the [MIT License](http://opensource.org/licenses/MIT)
