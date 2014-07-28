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

(defn ->envelope [destination payload]
  {:address destination :payload payload})

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
           (. tuples onNext (->envelope (get tuple "audience") (serialize tuple)))
           (reify-tuple tuple))))))

(defn addressed-to [puk]
  (fn [envelope]
    (let [destination-address (:address envelope)]
      (= destination-address puk))))

(defmacro subscriber-filter [a]
  `(~'publisher-field ~a))

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
                  (->> 
                    (reduce
                      (fn [tuples [f v]] (rx/filter #(= (get % f) v) tuples))
                      tuples-for-me
                      criteria)
                    (rx/map reify-tuple)))))))

(defn new-tuples [own-puk session]
  (reify TupleSpace
    (publisher [this]
      (new-tuple-publisher session {"author" own-puk}))
    (filter [this]
      (let [tuples-for-me (rx/filter (addressed-to own-puk) session)]
        (new-tuple-filter (rx/map (comp deserialize :payload) tuples-for-me))))))

(defn new-sneer-admin [session]
  (let [prik (atom nil)]
    (reify SneerAdmin
      (initialize [this new-prik]
        (swap! prik (fn [old] (do (assert (nil? old)) new-prik))))
      (sneer [this]
        (reify Sneer
          (tupleSpace [this]
            (new-tuples (.publicKey @prik) session)))))))

(defn new-session []
  (ReplaySubject/create))

