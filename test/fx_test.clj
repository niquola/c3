(ns fx-test
  (:require [fx :as sut]
            [clojure.test :refer :all]))


(defn mock-http [ctx {url :url}]
  {:body {:a 1} :status 200})

(defn mock-telegram [ctx {url :url}]
  )

(defn mock-k8s-req [ctx {url :url}]

  )

(def fxs
  {:http mock-http
   :telegramm mock-telegram})


(deftest test-fx
  (let [http (:http fxs)]

    (http {} {:url "https://github.com"})
    )

  )
