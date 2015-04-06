(ns sneer.impl
  (:require
   [sneer.conversation :refer [reify-conversations]]
   [sneer.contact :refer [create-contacts-state add-contact get-contacts find-contact problem-with-new-nickname]]
   [sneer.party :refer [party-puk reify-party produce-party! create-puk->party]]
   [sneer.profile :refer [produce-profile]])
  (:import
    [sneer Sneer PrivateKey]
    [sneer.tuples TupleSpace]
    (java.util Random)))

(defn new-sneer [^TupleSpace tuple-space ^PrivateKey own-prik]
  (let [own-puk (.publicKey own-prik)
        puk->party (create-puk->party)
        profiles (atom {})
        contacts-state (create-contacts-state tuple-space own-puk puk->party)
        contacts (get-contacts contacts-state)
        conversations (reify-conversations own-puk tuple-space contacts)
        self (reify-party own-puk)]

    (reify Sneer
      (self [_] self)

      (profileFor [_ party]
        (produce-profile tuple-space profiles party))

      (contacts [_]
        contacts)

      (problemWithNewNickname [_ new-nick party]
        (problem-with-new-nickname contacts-state new-nick party))

      (addContact [_ nickname party invite-code-received]
        (add-contact contacts-state nickname party invite-code-received))

      (findContact [_ party]
        (find-contact contacts-state party))

      (produceParty [_ puk]
        (produce-party! puk->party puk))

      (tupleSpace [_]
        tuple-space)

      (conversations [_]
        conversations))))
