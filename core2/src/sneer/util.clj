(ns sneer.util)

(defmulti handle (fn [_state event]
                   (event :type)))
