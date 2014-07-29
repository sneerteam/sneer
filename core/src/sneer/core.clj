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

(defmacro with-field [a]
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
        (with-field type)
        (with-field audience)
        (with-field payload)
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
          (with-field audience)
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
                  (rx/on-next subscriptions-for-peers criteria)
                  (. subscriber add
                    (. tuples subscribe subscriber))))))))))

(defn payloads [type envelopes]
  (->> envelopes
    (rx/map type)
    (rx/filter (complement nil?))
    (rx/map deserialize)))

(defn broadcast? [envelope]
  (-> envelope :address nil?))

(defn announce-peer [session puk]
  (rx/on-next session (->envelope nil :peer-announcement puk)))

(defn reify-tuple-space [own-puk session]
  
  (let [local-tuples (ReplaySubject/create)
        envelopes-for-me (rx/filter (addressed-to own-puk) session)
        tuples-for-me (payloads :tuple envelopes-for-me)
        subscriptions-for-me (payloads :subscription envelopes-for-me)
        
        broadcasts (rx/filter broadcast? session)
        peer-announcements (->>
                             (payloads :peer-announcement broadcasts)
                             (rx/filter #(not= own-puk %))
                             rx/distinct)
        subscriptions-for-peers (ReplaySubject/create)]
    
    (announce-peer session own-puk)
    
    (rx/subscribe
      peer-announcements
      (fn [peer-puk]
        ; subscribe to tuples for me coming from peer
        (rx/on-next session
                    (->envelope peer-puk :subscription {"audience" own-puk :sender own-puk}))
        
        ; filter subscriptions-for-peers by peer-puk
        ))
    
    (rx/subscribe
      subscriptions-for-me
      (fn [subscription]
        (let [sender (:sender subscription)
              criteria (dissoc subscription :sender)
              tuples-for-subscriber (->> local-tuples
                                      (filter-by criteria)
                                      (rx/map #(->envelope sender :tuple %)))]
          (println "subscription from" sender "to" own-puk "where" criteria)
          (rx/subscribe
            tuples-for-subscriber
            (fn [tuple]
              (println "sending" tuple)
              (rx/on-next session tuple))))))
    
    (reify TupleSpace
      (publisher [this] 
        (new-tuple-publisher local-tuples {"author" own-puk}))
      (filter [this]
        (new-tuple-filter tuples-for-me subscriptions-for-peers)))))

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

