(ns sneer.queries
  (:require [clojure.core.async :as async]
            [sneer.tuple.protocols :refer :all]
            [sneer.tuple-base-provider :refer :all]))

(defn- query-convo-tuples-by [tuple-base criteria author-puk audience-puk lease & [xform]]
  (let [old (async/chan 1 xform)
        new (async/chan 1 xform)
        criteria (assoc criteria
                        "author"   author-puk
                        "audience" audience-puk)]
    (query-with-history tuple-base criteria old new lease)
    [old new]))

(defn query-convo-tuples
  "Queries tuples from a conversation between `own-puk` and `contact-puk` using `criteria`.
   Returns a pair of channels `[old-tuples new-tuples]`."
  [tb criteria own-puk contact-puk lease & [xform]]
  (let [[old-sent new-sent] (query-convo-tuples-by tb criteria own-puk contact-puk lease xform)
        [old-rcvd new-rcvd] (query-convo-tuples-by tb criteria contact-puk own-puk lease xform)
        old-tuples (async/merge [old-sent old-rcvd])
        new-tuples (async/merge [new-sent new-rcvd])]
    [old-tuples new-tuples]))
