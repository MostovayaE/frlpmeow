(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *inventory*)
(def ^:dynamic *name*)
;;(def ^:dynamic health 10)
;; (def ^:dynamic gold 100)
(def gold (atom 100))
(def health (atom 10))
(def prompt "> ")
(def streams (ref {}))

(defn increase-gold [amount]
  (swap! gold + amount))
(defn decrease-gold [amount]
  (swap! gold - amount))
(defn decrease-health [amount]
  (swap! health - amount))
(defn increase-health [amount]
  (swap! health + amount))

(defn carrying? [thing]
  (some #{(keyword thing)} @*inventory*))
