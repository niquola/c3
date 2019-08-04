(ns fx
  (:require [org.httpkit.client]
            [cheshire.core]
            [clojure.string :as str]))


(defn http [ctx {url :url :as opts}]
  (let [resp @(org.httpkit.client/request opts)
        body (when-let [body (:body resp)]
               (if (string? body) body (slurp body)))]
    {:status (:status resp)
     :body body}))
