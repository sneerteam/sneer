(ns sneer.core
  (:require
   [rx.lang.clojure.core :as rx])
  (:import
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
   [rx.schedulers TestScheduler]
   [rx.subjects ReplaySubject]))

(defprotocol Disposable
  (dispose [resource]))

(defprotocol Network
  (connect
    [network puk]
    "Returns a `rx.subjects.Subject' with an Observable part for packets addressed to puk and
an Observer part that will send packets over from puk."))

(defmacro reify+
  "expands to reify form after macro expanding the body"
  [& body]
  `(reify ~@(map macroexpand body)))

(defmacro tuple-getter [g]
  `(~g [~'this]
       (get ~'tuple ~(name g))))

(defn reify-tuple [tuple]
  (reify+ Tuple
    (get [this key] (get tuple key))
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
  ([tuples-out proto-tuple]
    (letfn
      [(with [field value]
          (new-tuple-publisher tuples-out (assoc proto-tuple field value)))]
      (reify+ TuplePublisher
        (with-field type)
        (with-field audience)
        (with-field payload)
        (field [this field value]
           (with field value))
        (pub [this payload]
           (.. this (payload payload) pub))
        (pub [this]
           (rx/on-next tuples-out proto-tuple)
           (reify-tuple proto-tuple))))))

(defn filter-by [criteria tuples]
  (reduce
    (fn [tuples [f v]] (rx/filter #(= (get % f) v) tuples))
    tuples
    criteria))

(defn new-tuple-filter
  ([tuple-source subs-out] (new-tuple-filter tuple-source subs-out {}))
  ([tuple-source subs-out criteria]
    (letfn
        [(with [field value]
           (new-tuple-filter tuple-source subs-out (assoc criteria field value)))]
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
                                          tuple-source
                                          (filter-by criteria)
                                          (rx/map reify-tuple)
                                          (rx/subscribe-on scheduler))]
                             (rx/subscribe tuples #(rx/on-next subscriber %))
                             (. scheduler triggerActions)
                             (rx/on-completed subscriber)))))
          (tuples [this]
            (let [tuples
                  (->>
                    tuple-source
                    (filter-by criteria)
                    (rx/map reify-tuple))]

              (rx/observable*
                (fn [subscriber]
                  (rx/on-next subs-out criteria)
                  (. subscriber add
                    (. tuples subscribe subscriber))))))))))

(defn payloads [type envelopes]
  (->> envelopes
    (rx/map type)
    (rx/filter (complement nil?))))

(defn reify-tuple-space

  ([own-puk followees network]
     (reify-tuple-space own-puk followees (connect network own-puk) (ReplaySubject/create)))

  ([own-puk followees connection tuple-base]

     (let [tuples-in (rx/distinct (payloads :tuple connection))
           subs-in (payloads :subscription connection)
           subs-out (ReplaySubject/create)
           subscriptions-by-sender (atom {})]

       (rx/subscribe
        (->> subs-out
             (rx/flatmap
              (fn [criteria]
                (if (get criteria "author")
                  (rx/return criteria)
                  (rx/map #(assoc criteria "author" %) followees))))
             (rx/map #(->envelope (get % "author") :subscription (assoc % :sender own-puk))))
        #(rx/on-next connection %))

       (rx/subscribe
        subs-in
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
                        (->> tuple-base
                             (rx/filter #(let [audience (get % "audience")]
                                           (or (nil? audience) (= sender audience))))
                             (filter-by criteria)
                             (rx/map #(->envelope sender :tuple %)))
                        (partial rx/on-next connection))]

                   (assoc cur sender {:criteria criteria :subscription subscription}))))))))

       (rx/subscribe tuples-in
                     (partial rx/on-next tuple-base))

       (reify TupleSpace
         (publisher [this]
           (new-tuple-publisher tuple-base {"author" own-puk}))
         (filter [this]
           (new-tuple-filter tuple-base subs-out))))))
