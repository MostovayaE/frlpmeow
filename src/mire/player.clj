(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *inventory*)
(def ^:dynamic *name*)
(def ^:dynamic *class*)

(def prompt "> ")
(def streams (ref {}))

(defn carrying? [thing]
  (some #{(keyword thing)} @*inventory*))
