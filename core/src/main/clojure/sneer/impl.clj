(ns sneer.impl
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [observe-for-computation flatmapseq]]
   [sneer.conversation :refer [produce-conversation create-conversations-state conversations produce-conversation-with]]
   [sneer.contact :refer [create-contacts-state add-contact get-contacts find-contact problem-with-new-nickname]]
   [sneer.party :refer [party-puk new-party produce-party! create-puk->party]]
   [sneer.profile :refer [produce-profile]])
  (:import
   [sneer Sneer PrivateKey]
   [sneer.tuples TupleSpace]
   [rx.subjects BehaviorSubject]))

(defn new-sneer [^TupleSpace tuple-space ^PrivateKey own-prik]
  (let [own-puk (.publicKey own-prik)
        puk->party (create-puk->party)
        profiles (atom {})        
        conversation-menu-items (BehaviorSubject/create [])
        contacts-state (create-contacts-state tuple-space own-puk puk->party)        
        contacts (get-contacts contacts-state)
        conversations-state (create-conversations-state own-puk tuple-space contacts conversation-menu-items)]
    
    (let [self (new-party own-puk)]
        (reify Sneer
          (self [_] self)

          (profileFor [_ party]
            (produce-profile tuple-space profiles party))

          (contacts [_]
            contacts)
         
          (problemWithNewNickname [_ puk new-nick]
            (problem-with-new-nickname contacts-state puk new-nick))
       
          (addContact [_ nickname party]
            (add-contact contacts-state nickname party))
         
          (findContact [_ party]
            (find-contact contacts-state party))

          (conversationsContaining [_ type]
            (rx/never))
        
          (conversations [_]
            (conversations conversations-state))
          
          (produceConversationWith [_ party]
            (produce-conversation-with conversations-state party))
          
          (setConversationMenuItems [_ menu-item-list]
            (rx/on-next conversation-menu-items menu-item-list))

          (produceParty [_ puk]
            (produce-party! puk->party puk))
        
          (tupleSpace [_]
            tuple-space)))))
