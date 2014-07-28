(ns sneer.core
  (:require
    [rx.lang.clojure.core :as rx]
    [sneer.serialization :refer :all])
  (:import
    [sneer.admin SneerAdmin]
    [sneer Sneer PrivateKey]
    [sneer.tuples Tuple TupleSpace TuplePublisher TupleFilter]
    [rx.subjects ReplaySubject]))

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

(defmacro publisher-field [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))

(defn ->envelope [destination type payload]
  {:address destination type (serialize payload)})

(defn new-tuple-publisher
  ([tuples tuple]
    (letfn
      [(with [field value]
          (new-tuple-publisher tuples (assoc tuple field value)))]
      (reify+ TuplePublisher
        (publisher-field type)
        (publisher-field audience)
        (publisher-field payload)
        (field [this field value]
             (with field value))
        (pub [this payload]
           (.. this (payload payload) pub))
        (pub [this]
           (. tuples onNext (->envelope (get tuple "audience") :tuple tuple))
           (reify-tuple tuple))))))

(defn addressed-to [puk]
  (fn [envelope]
    (let [destination-address (:address envelope)]
      (= destination-address puk))))

(defmacro subscriber-filter [a]
  `(~'publisher-field ~a))

(defn on-subscriber [subscriber criteria session]
  (println criteria))

(defn filter-by [criteria tuples]
  (reduce
    (fn [tuples [f v]] (rx/filter #(= (get % f) v) tuples))
    tuples
    criteria))

(defn new-tuple-filter
  ([tuples-for-me] (new-tuple-filter tuples-for-me {}))
  ([tuples-for-me criteria]
    (letfn
        [(with [field value]
            (new-tuple-filter tuples-for-me (assoc criteria field value)))]
        (reify+ TupleFilter
          (subscriber-filter type)
          (subscriber-filter author)
          (subscriber-filter audience)
          (field [this field value] (with field value))
          (tuples [this]
            (let [tuples 
                  (->> 
                    tuples-for-me
                    (filter-by criteria)
                    (rx/map reify-tuple))
                  session {}]
              
              (rx/observable*
                (fn [subscriber]
                  (on-subscriber subscriber criteria session)
                  (. subscriber add
                    (. tuples subscribe subscriber))))))))))

(defn payloads [type envelopes]
  (->> envelopes
    (rx/map type)
    (rx/filter (complement nil?))
    (rx/map deserialize)))

(defn reify-tuple-space [own-puk session]
  (let [local-tuples (ReplaySubject/create)
        envelopes-for-me (rx/filter (addressed-to own-puk) session)
        tuples-for-me (payloads :tuple envelopes-for-me)
        subscriptions-for-me (payloads :subscription envelopes-for-me)]
    (rx/subscribe
      subscriptions-for-me
      (fn [subscription]
        (let [sender (:sender subscription)
              criteria (dissoc subscription :sender)
              tuples-for-subscriber (->> local-tuples
                                      (filter-by criteria)
                                      (rx/map #(->envelope sender :tuple %)))]
          (rx/subscribe tuples-for-subscriber (partial rx/on-next session)))))
    (reify TupleSpace
      (publisher [this]
        ; TODO: session becomes local-tuples after :subscription envelopes start to be sent 
        (new-tuple-publisher session {"author" own-puk}))
      (filter [this]
        (new-tuple-filter tuples-for-me)))))

(defn new-sneer-admin [session]
  (let [prik (atom nil)]
    (reify SneerAdmin
      (initialize [this new-prik]
        (swap! prik (fn [old] (do (assert (nil? old)) new-prik))))
      (sneer [this]
        (let [tuple-space (reify-tuple-space (.publicKey @prik) session)]
          (reify Sneer
            (tupleSpace [this] tuple-space)))))))

(defn new-session []
  (ReplaySubject/create))

