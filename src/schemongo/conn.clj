(ns schemongo.conn
 (:require
    [somnium.congomongo :as congo]))

(def ^:dynamic *conn* (atom nil))

(defn connect-uri! [uri]
  (reset! *conn* (congo/make-connection uri))
  (congo/set-write-concern @*conn* :safe)
  (congo/set-connection! @*conn*))
