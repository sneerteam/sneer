(ns sneer.core
  (:require
   [rx.lang.clojure.core :as rx])
  (:import
   [sneer.admin SneerAdmin]
   [sneer Sneer PrivateKey Party Contact]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
   [rx.schedulers TestScheduler]
   [rx.subjects ReplaySubject PublishSubject]))

(defprotocol Network
  (connect
    [network puk]
    "Returns a `rx.subjects.Subject' with an Observable part for packets addressed to puk and
an Observer part that will send packets over from puk.")
  (dispose [network]))

(defmacro reify+
  "expands to reify form after macro expanding the body"
  [& body]
  `(reify ~@(map macroexpand body)))

(defmacro tuple-getter [g]
  `(~g [~'this]
       (get ~'tuple ~(name g))))

(defn reify-tuple [tuple]
  (reify+
    Tuple
    (tuple-getter type)
    (tuple-getter audience)
    (tuple-getter author)
    (tuple-getter payload)))

(defmacro with-field [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))

(defn ->envelope [destination type payload]
  {:address destination type payload})

(defn new-tuple-publisher
  ([tuples tuple]
    (letfn
      [(with [field value]
          (new-tuple-publisher tuples (assoc tuple field value)))]
      (reify+ TuplePublisher
        (with-field type)
        (with-field audience)
        (with-field payload)
        (field [this field value]
             (with field value))
        (pub [this payload]
           (.. this (payload payload) pub))
        (pub [this]
           (rx/on-next tuples tuple)
           (reify-tuple tuple))))))

(defn filter-by [criteria tuples]
  (reduce
    (fn [tuples [f v]] (rx/filter #(= (get % f) v) tuples))
    tuples
    criteria))

(defn new-tuple-filter
  ([tuples-for-me subscriptions-for-peers] (new-tuple-filter tuples-for-me subscriptions-for-peers {}))
  ([tuples-for-me subscriptions-for-peers criteria]
    (letfn
        [(with [field value]
           (new-tuple-filter tuples-for-me  subscriptions-for-peers (assoc criteria field value)))]
        (reify+ TupleFilter
          (with-field type)
          (with-field author)
          (audience [this prik] (with "audience" (.publicKey prik)))
          (field [this field value] (with field value))
          (localTuples [this]
                       (rx/observable*
                         (fn [subscriber]
                           (let [scheduler (TestScheduler.)
                                 tuples (->>
                                          tuples-for-me
                                          (filter-by criteria)
                                          (rx/map reify-tuple)
                                          (rx/subscribe-on scheduler))]
                             (rx/subscribe tuples #(rx/on-next subscriber %))
                             (. scheduler triggerActions)
                             (rx/on-completed subscriber)))))
          (tuples [this]
            (let [tuples
                  (->>
                    tuples-for-me
                    (filter-by criteria)
                    (rx/map reify-tuple))]

              (rx/observable*
                (fn [subscriber]
                  (rx/on-next subscriptions-for-peers criteria)
                  (. subscriber add
                    (. tuples subscribe subscriber))))))))))

(defn payloads [type envelopes]
  (->> envelopes
    (rx/map type)
    (rx/filter (complement nil?))))

(defn reify-tuple-space [own-puk peers network]

  (let [connection (connect network own-puk)
        local-tuples (ReplaySubject/create)
        tuples-for-me (->>
                        (payloads :tuple connection)
                        rx/distinct)
        subscriptions-for-me (payloads :subscription connection)

        subscriptions-for-peers (ReplaySubject/create)]

    (rx/subscribe
      peers
      (fn [peer-puk]
        (rx/subscribe
            (->> subscriptions-for-peers
              (rx/map #(assoc % "author" peer-puk))
              (rx/map #(->envelope peer-puk :subscription (assoc % :sender own-puk))))
            #(rx/on-next connection %))))

    (let [subscriptions-by-sender (atom {})]

      (rx/subscribe
        subscriptions-for-me
        (fn [subscription]
          (let [sender (:sender subscription)
                criteria (dissoc subscription :sender)]
            (swap!
              subscriptions-by-sender
              (fn [cur]
                (let [existing (get cur sender)]

                  (when-let [{subscription :subscription} existing]
                      (.unsubscribe subscription))

                   ; TODO: combine existing criteria with new one
                   (let [subscription
                         (rx/subscribe
                           (->> local-tuples
                             (rx/filter #(let [audience (get % "audience")]
                                           (or (nil? audience) (= sender audience))))
                             (filter-by criteria)
                             (rx/map #(->envelope sender :tuple %)))
                           (partial rx/on-next connection))]

                     (assoc cur sender {:criteria criteria :subscription subscription})))))))))

    (rx/subscribe tuples-for-me
                  (partial rx/on-next local-tuples))

    (reify TupleSpace
      (publisher [this]
        (new-tuple-publisher local-tuples {"author" own-puk}))
      (filter [this]
        (new-tuple-filter local-tuples subscriptions-for-peers)))))
