(ns mire.commands
  (:require [clojure.string :as str]
            [mire.rooms :as rooms]
            [mire.player :as player]
            [mire.events :as events]))

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (str (:desc @player/*current-room*)
       "\nExits: " (keys @(:exits @player/*current-room*)) "\n"))
       ;;(str/join "\n" (map #(str "There is " % " here.\n")
         ;;                  @(:items @player/*current-room*)))))

(def ^:dynamic *current-event* (ref nil)) ; Используем ref для изменения значения

(defn change_current_event
  "Change the value of the *current-event* dynamic variable."
  [new-event]
  (dosync
    (ref-set *current-event* new-event)))

(defn move-item-to-inventory [event]
  (dosync
    (let [items (deref (:found-item event))] ;; Извлечение объектов из :found-item
      (alter player/*inventory* #(into % items))     ;; Добавление объектов в *inventory*
      (alter (:found-item event) (constantly #{}))  ;; Очистка :found-item после перемещения объектов
      (println "Items moved to inventory."))))


(defn inventory
  "See what you've got."
  []
  (println (str "You are carrying:"))
  (println (seq @player/*inventory*))
  (println (str " health: " @player/health ", gold: " @player/gold))
  )

;; (defn all-information
;; []
;; (println (commands/grab :sword)
;; (commands/grab :armour)
;; (commands/look))
;; (commands/inventory))

(defn discard
  "Put something down that you're carrying."
  [thing]
  (dosync
   (if (player/carrying? thing)
     (do (move-between-refs (keyword thing)
                            player/*inventory*
                            (:items @player/*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing "."))))

(defn random_event
  "dcdcdc"
  []
  (let [current-event @*current-event*]
    (println (str (:desc current-event)))
    (println (str "1. " (:option1 current-event)))
    (println (str "2. " (:option2 current-event)))
    (println (str "3. " (:option3 current-event)))
    (loop []
      (let [input (read-line)]
        (cond
          (= input "1")
          (do
            (println (:action1 current-event))
            (cond
                (= (:name @player/*current-room*) :tavern)
                  (do
                    (player/increase-gold 10)
                    (player/decrease-health 2)
                    )
                (= (:name @player/*current-room*) :city)
                  (do
                    (player/decrease-health 1)
                    )
                (= (:name @player/*current-room*) :forest)
                  (do
                    (player/increase-health 2)
                    )
                (= (:name @player/*current-room*) :shipwreck)
                  (do
                    (player/decrease-gold 70)
                    (player/decrease-health 1)
                    (discard :armour)
                    (discard :sword)
                    )
            ))
          (= input "2")
          (do
            (println (:action2 current-event))
            (cond
                (= (:name @player/*current-room*) :tavern)
                  (do
                    (player/decrease-gold 5)
                    )
                (= (:name @player/*current-room*) :city)
                  (do
                    (player/decrease-gold 10)
                    )
                (= (:name @player/*current-room*) :forest)
                  (do
                    (move-item-to-inventory current-event)
                    )
                (= (:name @player/*current-room*) :shipwreck)
                  (do
                    (player/decrease-health 4)
                    (discard :armour)
                    )
            ))
          (= input "3")
            (do
            (println (:action2 current-event))
            (cond
                (= (:name @player/*current-room*) :tavern)
                  (do
                    
                    )
                (= (:name @player/*current-room*) :city)
                  (do
                    (player/decrease-health 1)
                    (player/increase-gold 10)
                    )
                (= (:name @player/*current-room*) :forest)
                  (do
                    
                    )
                (= (:name @player/*current-room*) :shipwreck)
                  (do
                    (player/decrease-health 8)
                    
                    )
            ))
          :else
            (do
              (println "Please input 1, 2, or 3")
              (recur)))))
              (inventory)
              ))

(defn check-and-run-random-event []
  (let [random-number (inc (rand-int 3))] ; Генерируем случайное число от 1 до 2
    ;;(println "Generated Random Number:" random-number)
    (when (= random-number 3)
      (random_event (inc (rand-int 5))))))

(defn move
  "\"♬ We gotta get out of this place... ♪\" Give a direction."
  [direction]
  (dosync
   (let [target-name ((:exits @player/*current-room*) (keyword direction))
         target (@rooms/rooms target-name)]
     (if target
       (do
         (move-between-refs player/*name*
                            (:inhabitants @player/*current-room*)
                            (:inhabitants target))
         (ref-set player/*current-room* target)
        ;; (println (str (:name @player/*current-room*)))
        ;;  (if (= (str (:name @player/*current-room*)) ":tavern")
        ;;     (change_current_event :fight))
        (if (= (:name @player/*current-room*) :tavern)
    (change_current_event (@events/events :fight)))
        (if (= (:name @player/*current-room*) :city)
    (change_current_event (@events/events :taxes)))
        (if (= (:name @player/*current-room*) :forest)
    (change_current_event (@events/events :dancers)))
        (if (= (:name @player/*current-room*) :shipwreck)
    (change_current_event (@events/events :overload)))
        (if (not= (:name @player/*current-room*) :capital)
         (do(random_event)
         (look)))
         
         
                )
       "You can't go that way."))))

(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (rooms/room-contains? @player/*current-room* thing)
     (do (move-between-refs (keyword thing)
                            (:items @player/*current-room*)
                            player/*inventory*)
         (str "You picked up the " thing "."))
     (str "There isn't any " thing " here."))))

(defn discard
  "Put something down that you're carrying."
  [thing]
  (dosync
   (if (player/carrying? thing)
     (do (move-between-refs (keyword thing)
                            player/*inventory*
                            (:items @player/*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing "."))))


(defn detect
  "If you have the detector, you can see which room an item is in."
  [item]
  (if (@player/*inventory* :detector)
    (if-let [room (first (filter #((:items %) (keyword item))
                                 (vals @rooms/rooms)))]
      (str item " is in " (:name room))
      (str item " is not in any room."))
    "You need to be carrying the detector for that."))

(defn say
  "Say something out loud so everyone in the room can hear."
  [& words]
  (let [message (str/join " " words)]
    (doseq [inhabitant (disj @(:inhabitants @player/*current-room*)
                             player/*name*)]
      (binding [*out* (player/streams inhabitant)]
        (println message)
        (println player/prompt)))
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (str/join "\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))

;; Command data

(def commands {"move" move,
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east" (fn [] (move :east)),
               "west" (fn [] (move :west)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "say" say
               "help" help})

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
         (apply (commands command) args))
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))
