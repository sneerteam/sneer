(ns sneer.flux.conversation-store
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :refer [go chan close! <! >! sliding-buffer]]
            [sneer.flux.macros :refer :all]
            [sneer.tuple-base-provider :refer :all]
            [sneer.tuple.persistent-tuple-base :as ptb]
            [sneer.tuple.protocols :refer :all]
            [sneer.async :refer [go-trace link-chan-to-subscriber thread-chan-to-subscriber]])
  (:import [sneer.admin SneerAdmin]))

(defn create-summarization-state []
  {})

(defn- contact-puk [tuple]
  (tuple "party"))

(defn update-contact [contact state]
  (let [contact-id (contact-puk contact)
        contact-name (contact "payload")
        timestamp (contact "timestamp")]
    (assoc state contact-id {:name contact-name :summary "" :timestamp timestamp :unread 0})))

(defn update-message [own-puk message state]
  (let [author (message "author")
        contact-puk (if (= author own-puk) (message "audience") author)]
    (-> state (update contact-puk assoc
                      :summary (or (message "label") "")
                :timestamp (message "timestamp"))
              (update-in [contact-puk :unread] inc))))

(defn flip [f]
  (fn [x y] (f y x)))

(defn summarize [state]
  (->> state
       vals
       (sort-by :timestamp (flip compare))))

(defn summarization-state-from-contacts [contacts]
  (reduce
   (flip update-contact)
   (create-summarization-state)
   contacts))

(defn start-summarization-machine [own-puk tuple-base summaries-out lease]
  (let [tuples (chan)]

    ; link machine lifetime to lease
    (go (println "SUMMARIZATION STARTED")
        (<! lease)
        (close! tuples)
        (println "SUMMARIZATION STOPPED"))

    (go-trace

     ; create initial summarization state based on all contacts
     (let [contact-query {"type" "contact" "author" own-puk "audience" own-puk}
           existing-contacts (<! (ptb/query-all tuple-base contact-query))
           state (summarization-state-from-contacts existing-contacts)

           last-contact-id (-> existing-contacts last (get "id"))
           existing-contacts nil

           ; convenience functions
           query-tuples (fn [criteria]
                          (query-tuples tuple-base criteria tuples lease))

           query-messages (fn [contact-puk]
                            (let [message-query {"type" "message" ptb/last-by-id true}]
                              (query-tuples (assoc message-query "audience" own-puk "author" contact-puk))
                              ; there's a subtle issue here, since we are emitting two separate queries onto the same
                              ; channel there's no guarantee the tuples will be ordered by id
                              ; a good solution would be to extend query-tuples criteria to support :or clauses
                              (query-tuples (assoc message-query "author" own-puk "audience" contact-puk))))]

       ; keep an eye on messages from existing contacts
       (doseq [contact-puk (keys state)]
         (query-messages contact-puk))

       ; query new contact tuples
       (query-tuples (assoc contact-query ptb/after-id last-contact-id))

       ; update conversation summaries
       (loop [state state]
         (>! summaries-out (summarize state))
         (when-some [tuple (<! tuples)]
           (case (tuple "type")
             "contact"
             (if-some [puk (contact-puk tuple)]
               (do
                 ; keep an eye on messages from new contact
                 (query-messages puk)
                 ; update
                 (recur (update-contact tuple state)))
               ; ignore contacts without a party for now
               (recur state))

             "message"
             (recur
              (update-message own-puk tuple state)))))))))


; Java interface

(defn to-foreign-summary [{:keys [name summary timestamp unread]}]
  (sneer.flux.ConversationStore$Summary. name summary timestamp unread))

(defcomponent sneer.flux.ConversationStore [^SneerAdmin admin]

  (defn summaries [this]
    (rx/observable*
     (fn [^rx.Subscriber subscriber]
       (let [own-puk (.. admin privateKey publicKey)
             tuple-base (tuple-base-of admin)
             to-foreign (map (fn [summaries] (mapv to-foreign-summary summaries)))
             summaries-out (chan (sliding-buffer 1) to-foreign)
             lease (chan)]
         (link-chan-to-subscriber lease subscriber)
         (link-chan-to-subscriber summaries-out subscriber)
         (start-summarization-machine own-puk tuple-base summaries-out lease)
         (thread-chan-to-subscriber summaries-out subscriber "conversation summaries"))))))
