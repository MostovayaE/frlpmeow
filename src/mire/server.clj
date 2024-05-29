(ns mire.server
  (:require [clojure.java.io :as io]
            [server.socket :as socket]
            [mire.player :as player]
            [mire.commands :as commands]
            [mire.rooms :as rooms]
            [mire.events :as events]))

(defn- cleanup []
  "Drop all inventory and remove player from room and player list."
  (dosync
   (doseq [item @player/*inventory*]
     (commands/discard item))
   (commute player/streams dissoc player/*name*)
   (commute (:inhabitants @player/*current-room*)
            disj player/*name*)))

(defn- get-unique-player-name [name]
  (if (@player/streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn- mire-handle-client [in out]
  (binding [*in* (io/reader in)
            *out* (io/writer out)
            *err* (io/writer System/err)]

    ;; We have to nest this in another binding call instead of using
    ;; the one above so *in* and *out* will be bound to the socket
    
    (print "\nWhat is your name? ") (flush)
    (binding [player/*name* (get-unique-player-name (read-line))
              player/*current-room* (ref (@rooms/rooms :start))
              commands/*current-event* (ref (@events/events :dancers))
              player/*inventory* (ref #{})
              ;;player/health 10
              ;;player/gold 100
              ]
      (dosync
       (commute (:inhabitants @player/*current-room*) conj player/*name*)
       (commute player/streams assoc player/*name* *out*))
      ;;(if (= (str (:name @commands/*current-event*)) ":dancers")
 ;; (println "true")
  
  (println (str (:name @commands/*current-event*)))
      (println (commands/grab :sword) (commands/grab :armour) (commands/look)) (print player/prompt) (commands/inventory) (flush)
      
     (try
  (loop [input (read-line)]
    (when (and input (>= @player/health 0)) ; Проверка и health is greater than или equal to 0
      (println (commands/execute input))
      (.flush *err*)
      (print player/prompt) (flush)
      (if (>= @player/health 0)
        (if (= (:name @player/*current-room*) :capital)
          (do
            (println "You've reached the capital. You win!")
            (commands/inventory)
            (cleanup)) ; Завершение игры с победой в случае достижения "столицы"
          (recur (read-line))) ; Реурзивный вызов чтения команды в противном случае
        (println "Your health is below 0. Game over."))))
  (finally (cleanup))))))


(defn -main
  ([port rooms-dir events-dir]
     (rooms/add-rooms rooms-dir)
     (events/add-events events-dir)
     (defonce server (socket/create-server (Integer. port) mire-handle-client))
     (println "Launching Mire server on port" port))
  ([port] (-main port "resources/rooms" "resources/events"))
  ([] (-main 3333 "resources/rooms" "resources/events")))
