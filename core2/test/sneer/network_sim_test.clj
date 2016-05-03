(ns sneer.network-sim-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.network-sim :as subject]])

(defn- receive [peer packet]
  (reset! peer (packet :send)))

(facts "network-sim"
  (let [network (subject/network-sim)
        peer1 (atom nil)
        peer2 (atom nil)]
    (subject/join network :addr1 (partial receive peer1))
    (subject/join network :addr2 (partial receive peer2))

    (subject/send-packet network {:send "Hi!", :from :foo-addr, :to :addr1})
    @peer1 => "Hi!"

    (subject/send-packet network {:send "Hey", :from :foo-addr, :to :addr2})
    @peer2 => "Hey"))
