(ns mire.server
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [server.socket :as socket]
            [mire.player :as player]
            [mire.commands :as commands]
            [mire.rooms :as rooms]
            [mire.classes :as classes :refer [get-current-class]]
            [mire.shared :as shared]))

; defs
(def current-connections (atom 0))
(def max-connections 2)
(def player-roles (atom {})) ; Сохранение информации о роли каждого игрока
(def player-classes (atom {}))
(def ready-players (atom #{}))  ; Множество готовых игроков

(defn broadcast-message [message]
  (doseq [out (vals @player/streams)]
    (binding [*out* out]
      (println message)
      (flush))))

(defn broadcast-message-to-players [message]
  (doseq [[name out] @player/streams]
    (when (= (:role (@player-roles name)) :player)
      (binding [*out* out]
        (println message)
        (flush)))))

(defn get-player-class []
  (print "\nChoose your class (Vagabound, Ranger, Vagrant, Warrior, Scounderl): ") (flush)
  (let [class (read-line)]
    (if (some #{"Vagabound" "Ranger" "Vagrant" "Warrior" "Scounderl"} (str/split class #" "))
      class
      (do
        (println "Invalid class. Please choose again.")
        (recur)))))

(defn- get-player-role [is-spectator]
  (if is-spectator
    :spectator
    (do
      (print "Do you want to play? (Yes/No): ") (flush)
      (let [response (clojure.string/lower-case (read-line))]
        (cond
          (= response "yes") :player
          (= response "no") :spectator
          :else (do
                  (println "Invalid response. Please answer Yes or No.")
                  (recur false)))))))
                  
; END NEW

(def ^:dynamic *role*)

(defn- cleanup [player-name]
  (dosync
   (commute player/streams dissoc player-name))
  (swap! player-roles dissoc player-name)
  (when (= *role* :player)
    (swap! current-connections dec)
    (swap! ready-players disj player-name))
  (broadcast-message (str player-name " has disconnected. Current players: " @current-connections))
  (println "Player disconnected. Current players:" @current-connections))

(defn- get-unique-player-name [name]
  (if (@player/streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn start-game []
  (when (= (count @ready-players) max-connections)
    (broadcast-message "All players are ready. The game has started!")
    ))

(defn execute-command [input]
  (if (= input "start")
    (do
      (swap! ready-players conj player/*name*)
      (broadcast-message-to-players (str player/*name* " is ready."))
      (let [chosen-class (get-player-class)]
        (start-game)))
    (commands/execute input)))


(defn- get-current-class-command []
  (get-current-class @player-roles player/*name*))


(defn- mire-handle-client [in out]
  (let [is-spectator (>= @current-connections shared/max-connections)]
    (binding [*in* (io/reader in)
              *out* (io/writer out)
              *err* (io/writer System/err)]
      (print "\nWhat is your name? ") (flush)
      (let [player-name (get-unique-player-name (read-line))
            player-role (get-player-role is-spectator)
            player-class (if (= player-role :player) (get-player-class) nil)]
        (dosync
          (commute player/streams assoc player-name *out*)
          (shared/set-player-role player-name player-role)
          (when player-class
            (shared/set-player-class player-name player-class))) ; Сохранение класса игрока, если применимо
        (when (= player-role :player)
          (swap! current-connections inc)
          (let [role-msg (str player-name " has connected as a player"
                              (if player-class (str " with class " player-class ".") "."))]
            (shared/broadcast-message (str role-msg " Current players: " @current-connections)))

          ;; Проверка, достигло ли количество подключений max-connections
          (when (= @current-connections shared/max-connections)
            (shared/broadcast-message "All players have connected. Type 'start' to begin the game!")))

        (print player/prompt) 
        (flush)

        (try
          (loop [input (read-line)]
            (when input
              (let [response (commands/execute input player-name)]
                (when (not= response "Unknown command")
                  (shared/broadcast-message response))  ;; Передаем сообщение всем игрокам
                (when (= response "All players are ready. The game is starting!")
                  ;; Логика для начала игры
                  (println "The game has started!")))
              (.flush *err*)
              (print player/prompt) (flush)
              (recur (read-line))))
          (finally (cleanup player-name)))))))

; срабатывает 3 мейна снизу вверх

(defn -main
  ([port dir]
     (defonce server (socket/create-server (Integer. port) mire-handle-client))
     (println "Launching Mire server on port" port))

  ([port] (-main port "resources/rooms"))

  ([] (-main 3334)))