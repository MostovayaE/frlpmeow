(ns mire.events)

(def events (ref {}))

(defn load-event [events file]
  (let [event (read-string (slurp (.getAbsolutePath file)))]
    (conj events
          {(keyword (.getName file))
           {:name (keyword (.getName file))
            :desc (:desc event)
            :option1 (:option1 event)
            :option2 (:option2 event)
            :option3 (:option3 event)
            :action1 (:action1 event)
            :action2 (:action2 event)
            :action3 (:action3 event)
            ;;:found-item (:found-item event)
            :found-item (ref (or (:found-item event) #{}))
            :number-with-item (int (:number-with-item event))
            :number-with-heal (int (:number-with-heal event))
            :number-with-points (int (:number-with-points event))
            :number-with-move (:number-with-heal event)
            
            }})))

(defn load-events
  "Given a dir, return a map with an entry corresponding to each file
  in it. Files should be maps containing event data."
  [events dir]
  (dosync
   (reduce load-event events
           (.listFiles (java.io.File. dir)))))

(defn add-events
  "Look through all the files in a dir for files describing events and add
  them to the mire.events/events map."
  [dir]
  (dosync
   (alter events load-events dir)))

(defn event-contains?
  [event thing]
  (@(:found-item event) (keyword thing)))