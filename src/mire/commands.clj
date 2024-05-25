(ns mire.commands
  (:require [clojure.string :as str]
            [mire.rooms :as rooms]
            [mire.player :as player]
            [mire.classes :as classes]
            [mire.shared :as shared]))

;; Динамическая переменная для хранения роли игрока
(def ^:dynamic *role* nil)

;; Функция для получения строки с ролью пользователя
(defn get-player-role-string [player-name]
  (let [role (shared/get-player-role player-name)]
    (if role
      (str "Your current role is: " (name role))
      "Role is not set yet.")))

(defn get-player-class-string [player-name]
  (let [class (shared/get-player-class player-name)]
    (if class
      (str "Your current class is: " class)
      "Class is not set yet.")))

;; Функция для выполнения команд
(defn execute-command [input player-name]
  (let [player-role (shared/get-player-role player-name)
        command (str/split input #" ")]
    (if (= player-role :player) ; Проверяем, что роль - player
      (case (first command)
        "role" (get-player-role-string player-name)
        "class" (get-player-class-string player-name)
        "start" (do
                  (shared/add-ready-player player-name)
                  (let [ready-count (shared/ready-player-count)]
                    (if (= ready-count shared/max-connections)
                      (shared/broadcast-message "All players are ready. The game is starting!")
                      (str ready-count " players are ready."))))
        "Unknown command")
      "Access denied. You must be a player to execute commands.")))

;; Основная функция для обработки команд
(defn execute
  "Execute a command that is passed to us."
  [input player-name]
  (try
    (let [[command & args] (str/split input #" +")]
      (execute-command command player-name))
    (catch Exception e
      (.printStackTrace e (new java.io.PrintWriter *err*))
      "You can't do that!")))