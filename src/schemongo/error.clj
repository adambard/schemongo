(ns schemongo.error)

(defn bind-error [[val err] mf]
  ; Val is a tuple of (val, err), where error is an error message if an error
  ; occurred
  (if (nil? err)
    (if (vector? val)
      (apply mf val)
      (mf val))
    [val err]))

(defn unit-error [val]
  [val nil])
