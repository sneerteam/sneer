(ns sneer.notifications
  (:import [sneer.commons Container]
           [sneer.convos Notifications]
           [rx Observable]))

(defn reify-Notifications [^Container container]
  (reify Notifications
    (get [_]
      (Observable/just nil))))
