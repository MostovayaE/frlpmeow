(ns mire.classes
  (:require [mire.classes :as classes]))

(def classes
  {:warrior {:hp 100 :attack 10 :defense 5}
   :mage {:hp 60 :attack 15 :defense 2}
   :thief {:hp 80 :attack 8 :defense 3}})

(defn get-current-class [player-roles player-name]
  (if-let [role (:role (player-roles player-name))]
    (if (= role :player)
      (str "Your current class is: " (get-in classes [role]))
      "You are not playing. No class assigned.")
    "You are not registered."))

(defn get-class [class-name]
  (get classes class-name))
