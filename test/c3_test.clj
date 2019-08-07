(ns c3-test
  (:require [c3 :as sut]
            [clojure.test :refer :all]
            [clojure.string :as str]))

(defn mock-http [ctx {url :url :as req}]
  (println "REQ:" req)
  (cond
    (str/ends-with? url "c3.yaml")
    {:body (clj-yaml.core/generate-string {:a 1 :b 2}) 
     :status 200}

    :else
    {:status 404 :body (pr-str req)}))

(def ctx
  {:secret "4B81FEE3EF5877F899AB0325C0CFE786489433D82DC3909B22B6E638C81B5A36"
   :fx {:http mock-http}})


(deftest test-c3

  (def enc-key
    (:body (sut/handle ctx {:uri "/enc" :body "mysecretkey"})))

  (def dec-key
    (:body (sut/handle ctx {:uri "/dec" :body enc-key})))

  (is (= dec-key "mysecretkey"))

  (sut/handle ctx {:uri "/"
                   :request-method :post
                   :params {:key enc-key}
                   :body (cheshire.core/generate-string {:repository {:contents_url "http://gh.com/"}})})



  )

