(ns sneer.impl
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [observe-for-computation flatmapseq]]
   [sneer.conversation :refer [produce-conversation]]
   [sneer.contact :refer [create-contact-state add-contact get-contacts get-puk->contact]]
   [sneer.party :refer [party-puk new-party produce-party! create-puk->party]]
   [sneer.profile :refer [produce-profile]])
  (:import 
   [sneer Sneer PrivateKey Contact]
   [sneer.tuples TupleSpace]
   [rx.subjects BehaviorSubject]))

(defn new-sneer [^TupleSpace tuple-space ^PrivateKey own-prik ^rx.Observable followees]
  (let [own-puk (.publicKey own-prik)
        puk->party (create-puk->party)
        profiles (atom {})        
        conversation-menu-items (BehaviorSubject/create [])
        contact-state (create-contact-state tuple-space own-puk puk->party)
        contacts (get-contacts contact-state)]

    (rx/subscribe
      (->>
        contacts
        flatmapseq
        (rx/flatmap (fn [^Contact contact] (.. contact party publicKey observable))))
      (partial rx/on-next followees))
    
    (let [self (new-party own-puk)]
        (reify Sneer
          (self [this] self)

          (profileFor [this party]
            (produce-profile tuple-space profiles party))

          (contacts [this]
            contacts)
         
          (problemWithNewNickname [this new-nick]
            ;TODO
            )
       
          (addContact [this nickname party]
            (add-contact tuple-space contact-state nickname party own-puk))
         
          (findContact [this party]
            (get-puk->contact contact-state party))

          (conversationsContaining [this type]
            (rx/never))
        
          (conversations [this]
            (->>
              contacts
              (rx/map
                (partial map (fn [^Contact c] (produce-conversation tuple-space conversation-menu-items own-puk (.party c)))))))
          
          (produceConversationWith [this party] 
            (produce-conversation tuple-space conversation-menu-items own-puk party))
          
          (setConversationMenuItems [this menu-item-list]
            (rx/on-next conversation-menu-items menu-item-list))

          (produceParty [this puk]
            (produce-party! puk->party puk))
        
          (tupleSpace [this]
            tuple-space)))))
