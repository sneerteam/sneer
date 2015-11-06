(ns sneer.notification-core-test
  (:require [midje.sweet :refer :all]
            [clojure.string :refer [join]]))

(defn- notification [version unreads]
  (let [num (count unreads)
        unique (= num 1)]
    {:version  version
     :title    (if unique
                 "New Message"
                 (str num " New Messages"))
     :text     (join "\n" (map #(str (% :nick) ": " (% :preview)) unreads))
     :subtext  ""
     :convo-id (when unique (-> unreads first :id))}))

(defn notify [dismissed-version summaries]
  (let [version (summaries :version)
        unreads (remove #(empty? (% :unread)) (summaries :state))]
    (cond
      (>= dismissed-version version) nil
      (empty? unreads) nil
      :else (notification version unreads))))

(fact "Empty summary generates no notification"
  (notify 0 {:version 0 :state []}) => nil)

(fact "Already dismissed version generates no notification"
  (notify 4 {:version 4
             :state [{:id 1
                      :nick "Neide"
                      :timestamp 1
                      :preview "Hi"
                      :unread "*"}]}) => nil)

(fact "New version generates notification"
  (notify 0 {:version 1
             :state [{:id 1001
                      :nick "Neide"
                      :timestamp 34
                      :preview "Hi"
                      :unread "*"}]}) => {:version 1
                                          :title    "New Message"
                                          :text     "Neide: Hi"
                                          :subtext  ""
                                          :convo-id 1001})

(fact "With 2 messages"
  (notify 0 {:version 1
             :state [{:id 1001 :nick "Neide" :timestamp 34 :preview "Hi"    :unread "*"}
                     {:id 1002 :nick "Carla" :timestamp 37 :preview "Hello" :unread "*"}]})
  => {:version  1
      :title    "2 New Messages"
      :text     (str "Neide: Hi" "\n" "Carla: Hello")
      :subtext  ""
      :convo-id nil})

(fact "Only unread summaries should be present"
  (notify 0 {:version 1
             :state [{:id 1001 :nick "Neide" :timestamp 34 :preview "Hi"    :unread "*"}
                     {:id 1002 :nick "Maico" :timestamp 35 :preview "Hey"   :unread "" }
                     {:id 1003 :nick "Carla" :timestamp 37 :preview "Hello" :unread "?"}]})
  => {:version  1
      :title    "2 New Messages"
      :text     (str "Neide: Hi" "\n" "Carla: Hello")
      :subtext  ""
      :convo-id nil})