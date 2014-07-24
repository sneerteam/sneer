(ns sneer.core
  (:require
    [rx.lang.clojure.core :as rx]
    [sneer.serialization :refer :all])
  (:import
    [sneer.admin SneerAdmin]
    [sneer Sneer PrivateKey]
    [sneer.tuples Tuple Tuples TuplePublisher TupleSubscriber]
    [rx.subjects ReplaySubject]))

(defmacro reify+
  "expands to reify form after macro expanding the body"
  [& body]  
  `(reify ~@(map macroexpand body)))

(defmacro tuple-getter [g]
  `(~g [~'this]
       (get ~'attrs ~(name g))))

(defn ->tuple [attrs]
  (reify+
    Tuple
    (tuple-getter type)
    (tuple-getter audience)
    (tuple-getter author)
    (tuple-getter value)))

(defmacro publisher-attr [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))

(defn ->envelope [destination tuple]
  {:address destination :payload tuple})

(defn new-tuple-publisher
  ([tuples attrs]
    (letfn
      [(with [attr value]
          (new-tuple-publisher tuples (assoc attrs attr value)))]
      (reify+ TuplePublisher
        (publisher-attr type)
        (publisher-attr audience)
        (publisher-attr value)
        (put [this key value]
             (with key value))
        (pub [this value]
           (.. this (value value) pub))
        (pub [this]
           (. tuples onNext (->envelope (get attrs "audience") (serialize attrs)))
           (->tuple attrs))))))

(defn addressed-to [puk]
  (fn [envelope]
    (let [destination-address (:address envelope)]
      (or
        (nil? destination-address) ; public tuples
        (= destination-address puk)))))

(defmacro subscriber-filter [attr]
  `(~attr [~'this ~'expected]
     (new-tuple-subscriber
       ~'own-puk
       (rx/filter #(= (. % ~attr) ~'expected) ~'tuples))))

(defn new-tuple-subscriber [own-puk tuples]
  (reify+ TupleSubscriber
    (subscriber-filter type)
    (subscriber-filter author)
    (subscriber-filter audience)
	  (where [this key value]
          (new-tuple-subscriber
			       own-puk
			       (rx/filter #(= (. % key) value) tuples)))
    (tuples [this] tuples)))

(defn new-tuples [own-puk session]
  (reify Tuples
    (newTuplePublisher [this]
      (new-tuple-publisher session {"author" own-puk}))
    (newTupleSubscriber [this]
      (let [tuples-for-me (rx/filter (addressed-to own-puk) session)]
        (new-tuple-subscriber own-puk (rx/map (comp ->tuple deserialize :payload) tuples-for-me))))))

(defn new-sneer-admin [session]
  (let [prik (atom nil)]
    (reify SneerAdmin
      (initialize [this new-prik]
        (swap! prik (fn [old] (do (assert (nil? old)) new-prik))))
      (sneer [this]
        (reify Sneer
          (tuples [this]
            (new-tuples (.publicKey @prik) session)))))))

(defn new-session []
  (ReplaySubject/create))

