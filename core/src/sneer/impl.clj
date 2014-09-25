(ns sneer.impl
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [observe-for-computation flatmapseq]]
   [sneer.conversation :refer [reify-conversation]]
   [sneer.contact :refer [reify-contact duplicate-contact? create-contact-state]]
   [sneer.party :refer [party-puk new-party produce-party! create-puk->party]]
   [sneer.profile :refer [produce-profile]])
  (:import 
   [sneer Sneer PrivateKey Contact]
   [sneer.commons.exceptions FriendlyException]
   [sneer.tuples TupleSpace]
   [rx.subjects BehaviorSubject]))

(defn new-sneer [^TupleSpace tuple-space ^PrivateKey own-prik ^rx.Observable followees]
  (let [own-puk (.publicKey own-prik)
        puk->party (create-puk->party)
        profiles (atom {})
        conversation-menu-items (BehaviorSubject/create [])
        contact-state (create-contact-state tuple-space own-puk puk->party)]

    (rx/subscribe
      (->>
        (contact-state :observable-contacts)
        ;observe-for-computation
        flatmapseq
        (rx/flatmap (fn [^Contact c] (.. c party publicKey observable))))
      (partial rx/on-next followees))
    
    (letfn [(add-contact [nickname party]
              (swap! (contact-state :puk->contact)
                     (fn [cur]
                       (when (->> cur vals (some (partial duplicate-contact? nickname party)))
                         (throw (FriendlyException. "Duplicate contact!")))
                       (assoc cur
                         (party-puk party)
                         (reify-contact nickname party)))))
            
            (produce-conversation [party]
              (reify-conversation tuple-space (.asObservable conversation-menu-items) own-puk party))]

      (let [self (new-party own-puk)]
        (reify Sneer
          (self [this] self)

          (profileFor [this party]
            (produce-profile tuple-space profiles party))

          (contacts [this]
            (contact-state :observable-contacts))
         
          (problemWithNewNickname [this new-nick]
            ;TODO
            )
       
          (addContact [this nickname party]
            (add-contact nickname party)
            (.. tuple-space
                publisher
                (audience own-puk)
                (type "contact")
                (field "party" (party-puk party))
                (pub nickname)))
         
          (findContact [this party]
            (get @(contact-state :puk->contact) (party-puk party)))

          (conversationsContaining [this type]
            (rx/never))
        
          (conversations [this]
            (->>
              (contact-state :observable-contacts)
              (rx/map
                (partial map (fn [^Contact c] (produce-conversation (.party c)))))))
          
          (produceConversationWith [this party] 
            (produce-conversation party))
          
          (setConversationMenuItems [this menu-item-list]
            (rx/on-next conversation-menu-items menu-item-list))

          (produceParty [this puk]
            (produce-party! puk->party puk))
        
          (tupleSpace [this]
            tuple-space))))))
