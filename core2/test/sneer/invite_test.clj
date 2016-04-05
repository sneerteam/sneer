(ns sneer.invite-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.network-sim :as net]
    [sneer.sneer-test-util :refer :all]
    [sneer.core2 :refer :all]
    [sneer.streem :refer :all]])

(facts "Invite"
  (let [network (net/network-sim)
        neide-ui (atom nil)
        carla-ui (atom nil)
        neide (join network "Neide da Silva" #(reset! neide-ui %))
        carla (join network "Carla Costa"    #(reset! carla-ui %))]

    (handle! neide {:type :contact-new, :nick "Carla"})

    (let [n->c-id (get-in @neide-ui [:convo-list 0 :contact-id])
          _ (handle! neide {:type :view, :path [:convo n->c-id]})
          invite (get-in @neide-ui [:convo :invite])]

      (fact "Invite appears in convo-list and convo"
        invite => some?
        (get-in @neide-ui [:convo-list 0 :invite]) => invite)

      (fact "Invite disappears on sender's side after being accepted by receiver"
        (handle! carla {:type   :contact-invite-accept
                        :invite invite})

        (get-in @neide-ui [:convo :invite]) => nil
        (get-in @neide-ui [:convo-list 0 :invite]) => nil)

      (fact "Inviter's name appears as contact nick"
        (get-in @carla-ui [:convo-list 0 :nick]) => "Neide da Silva")))

;    "TODO" => "Duplicate nicks not allowed"
;    "TODO" => "Duplicate puks not allowed"
  )
