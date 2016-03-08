(ns reagent-spike.prod
  (:require [reagent-spike.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
