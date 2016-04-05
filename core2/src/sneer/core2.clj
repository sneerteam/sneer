(ns sneer.core2
  (require
    [sneer.model :refer :all]
    [sneer.util.core :refer [handle prepend assoc-some]]
    [sneer.streem :refer :all]))

(defn- convo-list [model]
  (->> model :convos :id->summary vals (sort-by :last-event-id) reverse vec))

(defn- chat [streems contact-id]
  (catch-up! streems conj [] contact-id))

(defn- convo [streems model contact-id]
  (-> model
    (get-in [:convos :id->summary contact-id])
    (select-keys [:contact-id :nick :invite])
    (assoc :chat (chat streems contact-id))))

(defn- view [streems model [activity contact-id]]
  (cond-> {:convo-list (convo-list model)
           :profile (:profile model)}
    (= activity :convo)
    (assoc :convo (convo streems model contact-id))))

(defn- update-ui! [sneer model]
  ((sneer :ui-fn) (view (sneer :streems) model @(sneer :view-path))))

(defn- streem-id [event]
  (case (event :type)
    :msg-send (event :contact-id)
    nil))

(defn- update-network! [sneer model]

  )

(defn- catch-up-model! [streems]
  (catch-up! streems handle))

(defn- model! [sneer]
  (catch-up-model! (sneer :streems)))

(defn- random-bytes [sneer array-size]
  ((-> sneer :crypto-fns :generate-random-bytes) array-size))

(defn- deterministic!
  "Adds information such as timestamp and random bytes when necessary."
  [sneer event]
  (if (-> event :type (= :contact-new))
    (assoc event :random-bytes (random-bytes sneer 8))
    event))

(defn handle! [sneer event]
  (let [event (deterministic! sneer event)
        streems (sneer :streems)]
    (if (= (event :type) :view)
      (reset! (sneer :view-path) (event :path))
      (append! streems event (streem-id event)))
    (let [model (catch-up-model! streems)]
      (update-network! sneer model)
      (update-ui! sneer model))))

(defn- keys-init-if-necessary [sneer model]
  (when-not (:key-pair model)
    (let [key-pair ((get-in sneer [:crypto-fns :generate-key-pair]))]
      (handle! sneer {:type :keys-init
                      :puk  (key-pair "puk")
                      :prik (key-pair "prik")}))))

(defn puk [sneer]
  (-> sneer model! puk2))

(defn sneer [streems ui-fn server> crypto-fns]
  (let [sneer {:streems    streems
               :ui-fn      ui-fn
               :server>    server>
               :crypto-fns crypto-fns
               :view-path  (atom nil)}
        model (model! sneer)]
    (keys-init-if-necessary sneer model)
    (update-ui! sneer model)
    sneer))
