(ns sneer.contact
  (require
    [sneer.util :refer [handle]]))

(defmethod handle :contact-new [state data]
  (let [contact (select-keys data [:nick])]
    (update-in state [:view :convo-list] conj contact)))
