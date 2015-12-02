(ns sneer.neide-and-carla
  (:require [sneer.integration-test-util :refer [sneer! connect!]]
            [sneer.rx-test-util :refer [<next]])
  (:import [sneer.convos Convos]))

(defn neide-and-carla []
  (let [neide (sneer!)
        carla (sneer!)
        _     (connect! neide carla)
        n-convos ^Convos (neide Convos)
        n->c-id  (<next (. n-convos startConvo "Carla"))
        n->c     (<next (.getById n-convos n->c-id))
        c-convos ^Convos (carla Convos)
        _        (.acceptInvite c-convos
                                "Neide"
                                (.inviteCodePending n->c))
        c->n-id  (<next (.findConvo c-convos (.inviteCodePending n->c)))
        c->n     (<next (.getById c-convos c->n-id))]
    {:neide neide
     :n->c n->c
     :carla carla
     :c->n c->n}))
