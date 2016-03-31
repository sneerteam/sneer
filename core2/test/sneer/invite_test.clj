(ns sneer.invite-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.core2 :refer :all]
    [sneer.streem :refer :all]])

(defn sneer-community []
  (atom nil))

(defn server> [community packet] ;packet {:from own-puk :send tuple :to puk}
  ()
  )

(defn dummy-key [prefix key-type]
  (str (subs prefix 0 3) "-" key-type))

(defn join [community ui-fn own-name]
  (let [server> (partial server> community)
        crypto-fns {:generate-key-pair #(do {"prik" (dummy-key own-name "prik")
                                             "puk"  (dummy-key own-name "puk")})}
        member (sneer ui-fn (streems) server> crypto-fns)]
    (swap! community assoc (puk member) member)
    member))

#_(facts "Invite"
  (let [subject (sneer-community)
        neide-ui (atom nil)
        carla-ui (atom nil)
        neide (join subject #(reset! neide-ui %) "Neide da Silva")
        carla (join subject #(reset! carla-ui %) "Carla Costa")]

    (handle! neide {:type :contact-new, :nick "Carla"})

    (let [n->c-id (get-in @neide-ui [:convo-list 0 :contact-id])
          _ (handle! neide {:type :view, :path [:convo n->c-id]})
          invite (get-in @neide-ui [:convo :invite])]

      (fact "Invite appears in convo-list and convo"
        invite => some?
        (get-in @neide-ui [:convo-list 0 :invite]) => invite)

      (fact "Carla starts with no contacts"
        (get-in @carla-ui [:convo-list]) => [])

      (fact "Invite disappears on sender's side after being accepted by receiver"
        (handle! carla {:type   :contact-invite-accept
                        :invite invite})

        (get-in @neide-ui [:convo :invite]) => nil
        (get-in @neide-ui [:convo-list 0 :invite]) => nil)

      "TODO" => "Encode name in invite"

      (fact "Inviter's own name appears as contact nick"
        (get-in @carla-ui [:convo-list 0 :nick]) => "Neide da Silva"))))
