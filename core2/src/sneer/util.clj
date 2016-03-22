(ns sneer.util)

(defmulti handle (fn [_state event]
                   (event :type)))

(defn update-in
  "Updates a value in a vector of maps where test-key = test-val.
  update-key is the field being updated with the value of update-val"
  [mapvec [test-key test-val] [update-key update-val]]
  (reduce
   (fn [v idx]
     (if (= (get-in v [idx test-key]) test-val)
       (assoc-in v [idx update-key] update-val)
       v))
   mapvec
   (range (count mapvec))))
