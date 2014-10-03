(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [atom->observable]]
   [sneer.party :refer [party-puk]]
   [sneer.commons :refer [now]])
  (:import
   [sneer PublicKey Party Conversation Message]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace]
   [java.text SimpleDateFormat]))




;(defn comp [t1 t2]
;  (let [c (compare (:tr t1) (:tr t2))]
;    (if (not= c 0)
;      c
;      (let [c (compare (:tc t1) (:tc t2))]
;        (if (not= c 0)
;          c
;          (compare (:id t1) (:id t2)))))))

;(defn new-comp [t1 t2]
;  (let [c1 (compare (:tr t1) (:tr t2))
;        c2 (compare (:tc t1) (:tc t2))
;        c3 (compare (:id t1) (:id t2))]
;    (if (not= c1 0)
;      c1
;      (if (not= c2 0)
;        c2
;        c3))))


(def simple-date-format (SimpleDateFormat. "HH:mm"))

(defn format-date [time] (.format simple-date-format time))

(defn tuple->message [own-puk ^Tuple tuple]
  (let [created (.timestampCreated tuple)
        received (.timestampReceived tuple)
        type (.type tuple)
        label (.get tuple "label")
        content (if (= type "message") (.payload tuple) (if label label type))
        own? (= own-puk (.author tuple))]
    
    (reify Message
      (isOwn [this] own?)
      (content [this] content)
      (timestampCreated [this] created)
      (timestampReceived [this] received)
      (timeCreated [this] (format-date created))
      (tuple [this] tuple))))

(defn reify-conversation [^TupleSpace tuple-space ^rx.Observable conversation-menu-items ^PublicKey own-puk ^Party party]
  (let [^PublicKey party-puk (party-puk party)
        messages (atom [])
        observable-messages (atom->observable messages)
        message-filter (.. tuple-space filter (field "conversation?" true))]
    
    (rx/subscribe
      (rx/subscribe-on
        (rx.schedulers.Schedulers/io)
        (rx/merge
          (.. message-filter (author own-puk) (audience party-puk) tuples)
          (.. message-filter (author party-puk) (audience own-puk) tuples)))
      #(swap! messages conj (tuple->message own-puk %)))
    
    (reify
      Conversation      
      (party [this] party)
      
      (messages [this]
        observable-messages)
      
      (sendMessage [this content]
        (..
          tuple-space
          publisher
          (field "conversation?" true)
          (field "timestampCreated" (now))
          (audience party-puk)
          (type "message")
          (pub content)))
      
      (mostRecentMessageContent [this]
        (.observed (ObservedSubject/create "hello")))
      
      (mostRecentMessageTimestamp [this]
        (.observed (ObservedSubject/create (now))))
    
      (menu [this] conversation-menu-items)
    
      (unreadMessageCount [this] (rx.Observable/just 1))
    
      (unreadMessageCountReset [this]))))

(defn produce-conversation [tuple-space conversation-menu-items own-puk party]
  (reify-conversation tuple-space (.asObservable conversation-menu-items) own-puk party))
