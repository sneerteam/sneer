(ns sneer.notifications
  (:require [rx.lang.clojure.core :as rx]
            [clojure.core.async :refer [chan pipe]]
            [sneer.rx :refer [close-on-unsubscribe! pipe-to-subscriber! shared-latest]])
  (:import [sneer.commons Container]
           [sneer.convos Notifications Notifications$Notification]
           [sneer.interfaces ConvoSummarization]
           [rx Observable Subscriber]))

(defn- to-foreign-notification [{:keys [convo-id title text sub-text]}]
  (Notifications$Notification. convo-id title text sub-text))

(defn notification [summaries]
  {:convo-id 0
   :title ""
   :text (apply str (map :preview summaries))
   :sub-text ""})

(defn- to-foreign [summaries]
  (let [unread (->> summaries (filter #(= "*" (:unread %))))]
    (if (empty? unread)
      :nil
      (to-foreign-notification (notification unread)))))

(defn- notifications* [^ConvoSummarization summarization]
  (shared-latest
    (rx/observable*
      (fn [^Subscriber subscriber]
        (let [in (.slidingSummaries summarization)
              out (chan 1 (map to-foreign))]
          (pipe in out)
          (close-on-unsubscribe! subscriber in out)
          (pipe-to-subscriber! out subscriber "notifications"))))))

(defn reify-Notifications [^Container container]
  (let [summarization (.produce container ConvoSummarization)
        notifications (notifications* summarization)]
    (reify Notifications
      (get [_] notifications))))
