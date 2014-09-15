(ns sneer.core
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [filter-by observe-for-io]]
   [sneer.serialization :refer [roundtrip]])
  (:import
   [sneer PrivateKey PublicKey]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
   [rx.schedulers TestScheduler]
   [rx.subjects ReplaySubject]))

(defprotocol Disposable
  (dispose [resource]))

(extend-protocol Disposable
  java.io.Closeable
  (dispose [closeable]
    (.close closeable)))

(defprotocol Network
  (connect
    [network puk]
    "Returns a `rx.subjects.Subject' with an Observable part for packets
addressed to puk and an Observer part that will send packets over from
puk." ))

(defprotocol TupleBase
  "A backing store for tuples (represented as maps)."

  (store-tuple ^Void [this tuple]
    "Stores the tuple represented as map.")

  (query-tuples ^rx.Observable [this criteria keep-alive]
    "Filters tuples by the criteria represented as a map of
field/value. When keep-alive is true the observable will keep emitting
new tuples as they are stored otherwise it will complete." )
  
  (restarted ^TupleBase [this]))


(defn copy-of [observable]
  (let [copy (ReplaySubject/create)]
    (rx/subscribe observable #(rx/on-next copy %))
    copy))


(extend-protocol TupleBase
  rx.subjects.Subject
  (store-tuple [subject tuple]
    (rx/on-next subject tuple))
  (query-tuples [subject criteria keep-alive]
    (if keep-alive
      (filter-by criteria subject)
      (rx/observable*
       (fn [subscriber]
         (let [scheduler (TestScheduler.)
               tuples (->>
                       subject
                       (filter-by criteria)
                       (rx/subscribe-on scheduler))]
           (rx/subscribe tuples #(rx/on-next subscriber %))
           (. scheduler triggerActions)
           (rx/on-completed subscriber))))))
  (restarted [this]
    (rx/on-completed this)
    (copy-of this)))


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
    (tuple-getter payload)
    (toString [this] (str tuple))))

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
           (let [tuple (roundtrip proto-tuple)]
             (store-tuple tuples-out tuple)
             (rx/return
               (reify-tuple tuple))))))))

(defn new-tuple-filter
  ([tuple-base subs-out] (new-tuple-filter tuple-base subs-out {}))
  ([tuple-base subs-out criteria]
    (letfn
        [(with [field value]
           (new-tuple-filter tuple-base subs-out (assoc criteria field value)))
         (query-tuple-base [keep-alive]
           (rx/map reify-tuple (query-tuples tuple-base criteria keep-alive)))]

        (reify+ TupleFilter
          (with-field type)
          (with-field author)
          (^TupleFilter audience [this ^PrivateKey prik]
            (with "audience" (.publicKey prik)))
          (^TupleFilter audience [this ^PublicKey puk]
            (with "audience" puk))
          (field [this field value] (with field value))
          (localTuples [this]
            (query-tuple-base false))
          (tuples [this]
            (rx/observable*
              (fn [^rx.Subscriber subscriber]
                (rx/on-next subs-out criteria)
                (let [^rx.Observable tuples (query-tuple-base true)]
                  (. subscriber add
                    (. tuples subscribe subscriber))))))))))

(defn payloads [type envelopes]
  (->> envelopes
    (rx/map type)
    (rx/filter (complement nil?))))

(defn get-author [criteria]
  (get criteria "author"))

(defn reify-tuple-space [own-puk tuple-base connection followees]

  (let [tuples-in (rx/distinct (payloads :tuple connection))
        subs-in (payloads :subscription connection)
        subs-out (ReplaySubject/create)
        subscriptions-by-sender (atom {})
        distinct-followees (ReplaySubject/create)]

    (rx/subscribe 
      (rx/distinct followees) 
      (partial rx/on-next distinct-followees))
    
    (rx/subscribe
      (->>
        subs-out
        observe-for-io
        (rx/filter #(not= (get-author %) own-puk))
        (rx/distinct)
        (rx/flatmap
         (fn [criteria]
           (if (get-author criteria)
             (rx/return criteria)
             (rx/map #(assoc criteria "author" %) distinct-followees))))
        (rx/map #(->envelope (get-author %) :subscription (assoc % :sender own-puk)))
        (rx/do #(println "[SUBS-OUT(" own-puk ")]" %)))
      #(rx/on-next connection %))

    (rx/subscribe
      (->>
        subs-in
        (rx/distinct)
        (rx/do #(println "[SUBS-IN(" own-puk ")]" %)))
      (fn [subscription]
        (let [sender (:sender subscription)
              criteria (dissoc subscription :sender)]
          (swap!
           subscriptions-by-sender
           (fn [cur]
             (let [existing (get cur sender)]
               ; TODO: unsubscribe
               ; TODO: combine existing criteria with new one
               (let [subscription
                     (rx/subscribe
                       (->>
                         (query-tuples tuple-base criteria true)
                         observe-for-io
                         ; TODO: extend query-tuples to support the condition below
                         (rx/filter #(let [audience (get % "audience")]
                                       (or (nil? audience) (= sender audience))))
                         (rx/map #(->envelope sender :tuple %)))
                       (partial rx/on-next connection))]

                 (assoc cur sender (conj existing {:criteria criteria :subscription subscription})))))))))

    (rx/subscribe tuples-in
                  (partial store-tuple tuple-base))

    (reify TupleSpace
      (publisher [this]
        (new-tuple-publisher tuple-base {"author" own-puk}))
      (filter [this]
        (new-tuple-filter tuple-base subs-out)))))
