Schemongo
=============

A mongo schema library with two features of note:

1. Validators for field types, including enumerable field types and BSON ObjectIds, and
2. A model macro and functions to shortcut the validation and creation/updating/deletion of data

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

Creates CRUD functions for a model named <name>, stored in <coll> and validated against <schema>

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

Validate <value> for <field> as a <type>, with <args> as arguments to the validator.
Returns [true nil] if successful and [nil <error message>] if not.

**(validate-data data schema)**

Validate all fields of <data> against <schema>. Strips any fields not in <schema> from <data>.

License
========

Licensed under the [MIT License](http://opensource.org/licenses/MIT)
