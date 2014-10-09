(ns sneer.impl
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [observe-for-computation flatmapseq]]
   [sneer.conversation :refer [produce-conversation create-conversations-state conversations produce-conversation-with]]
   [sneer.contact :refer [create-contacts-state add-contact get-contacts find-contact problem-with-new-nickname]]
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
        contacts-state (create-contacts-state tuple-space own-puk puk->party)        
        contacts (get-contacts contacts-state)
        conversations-state (create-conversations-state own-puk tuple-space contacts conversation-menu-items)]

    (rx/subscribe
      (->>
        contacts
        flatmapseq
        (rx/flatmap (fn [^Contact c] (.. c party publicKey observable))))
      (partial rx/on-next followees))
    
    (let [self (new-party own-puk)]
        (reify Sneer
          (self [this] self)

          (profileFor [this party]
            (produce-profile tuple-space profiles party))

          (contacts [this]
            contacts)
         
          (problemWithNewNickname [this new-nick]
            (problem-with-new-nickname contacts-state new-nick))
       
          (addContact [this nickname party]
            (add-contact contacts-state nickname party))
         
          (findContact [this party]
            (find-contact contacts-state party))

          (conversationsContaining [this type]
            (rx/never))
        
          (conversations [this]
            (conversations conversations-state))
          
          (produceConversationWith [this party]
            (produce-conversation-with conversations-state party))
          
          (setConversationMenuItems [this menu-item-list]
            (rx/on-next conversation-menu-items menu-item-list))

          (produceParty [this puk]
            (produce-party! puk->party puk))
        
          (tupleSpace [this]
            tuple-space)))))
