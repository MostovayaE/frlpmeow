(ns mire.shared
    (:require [mire.player :as player]))

(def player-roles (atom {}))  ; Словарь имён и их ролей
(def ready-players (atom #{}))
(def max-connections 2)

(defn get-player-role [player-name]
  (get-in @player-roles [player-name :role]))

(defn set-player-role [player-name role]
  (swap! player-roles assoc player-name {:role role}))

(defn get-player-class [player-name]
  (get-in @player-roles [player-name :class]))

(defn set-player-class [player-name class]
  (swap! player-roles assoc-in [player-name :class] class))

(defn add-ready-player [player-name]
  (swap! ready-players conj player-name))

(defn ready-player-count []
  (count @ready-players))

  (defn broadcast-message [msg]
  (doseq [[_ out] @player/streams]
    (.write out (str msg "\r\n"))
    (.flush out)))