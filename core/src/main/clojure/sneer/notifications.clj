(ns sneer.notifications
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :refer [chan pipe]]
            [clojure.string :as str]
            [sneer.rx :refer [close-on-unsubscribe! pipe-to-subscriber! shared-latest]]
            [sneer.async :refer [debounce]]
            [sneer.interfaces])
  (:import [sneer.commons Container]
           [sneer.convos Notifications Notifications$Notification]
           [sneer.interfaces ConvoSummarization]
           [rx Subscriber]))

(def ^:dynamic *debounce-timeout* 1500)

(defn format-notification-text [unread]
  (str/join "\n"
            (map #(str (:nick %) ": " (:preview %)) unread)) )

(defn notification [unread]
  (if (= (count unread) 1)
    (let [[{:keys [id nick preview]}] unread]
      (Notifications$Notification. (long id) nick preview ""))
    (Notifications$Notification. nil "New Messages" (format-notification-text unread) "")))

(defn- to-foreign [summaries]
  (let [unread (->> summaries (remove #(-> % :unread empty?)))]
    (if (empty? unread)
      :nil
      (notification unread))))

(defn- notifications* [^ConvoSummarization summarization]
  (shared-latest
    (rx/observable*
      (fn [^Subscriber subscriber]
        (let [in (.slidingSummaries summarization)
              out (chan 1 (map to-foreign))]
          (debounce in out *debounce-timeout*)
          (close-on-unsubscribe! subscriber in out)
          (pipe-to-subscriber! out subscriber "notifications"))))))

(defn reify-Notifications [^Container container]
  (let [summarization (.produce container ConvoSummarization)
        notifications (notifications* summarization)]
    (reify Notifications
      (get [_] notifications))))
