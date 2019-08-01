(ns ci3
  (:require [org.httpkit.server]
            [org.httpkit.client]
            [cheshire.core]
            [aes]
            [clj-yaml.core]
            [clojure.string :as str])
  (:import
   io.kubernetes.client.ApiClient
   io.kubernetes.client.ApiException
   io.kubernetes.client.Configuration
   io.kubernetes.client.apis.CoreV1Api
   io.kubernetes.client.models.V1Pod
   io.kubernetes.client.models.V1PodList
   io.kubernetes.client.util.Config

   java.io.BufferedReader
   java.io.InputStreamReader

   io.kubernetes.client.Exec))

(defonce client (atom nil))



(defn exec [cl ns pod cmd]
  (let [exec (io.kubernetes.client.Exec. cl)
        proc (.exec exec ns pod  (into-array cmd) true true)
        out (-> proc
                .getInputStream
                java.io.InputStreamReader.
                java.io.BufferedReader.)]
    (loop []
      (when-let [ l (.readLine out)]
        (println l)
        (recur)))))

(defn gh-file [url key commit file]
  (let [uri (str url file)
        _ (println "Loading " uri "?ref=" commit "WITH KEY " key)
        resp  @(org.httpkit.client/get uri
                                        {:query-params {:ref commit}
                                         :headers {"Authorization" (str "token " key)
                                                   "Accept" "application/vnd.github.v3.raw"}})]
    (println resp)
    (if-let [body (:body resp)]
      (if (string? body)
        body
        (slurp body))
      (println "RES:" resp))))

(defn hello [ctx req]
  {:status 300
   :body "
<html>

<body>
<center><h1>Welcome to CI3</h1></center>
</body>


</html>"})

(defn parse-params [qs]
  (reduce (fn [acc p]
            (let [[k v] (str/split p #"=")]
              (assoc acc (keyword k) v)))
          {} (str/split qs #"&")))

(defn hook [{secret :secret} req]
  (let [params (parse-params (:query-string req))
        body (cheshire.core/parse-string (slurp (:body req)) keyword)
        url  (str/replace (get-in body [:repository :contents_url]) #"\{\+path\}$" "/")
        ref "master"
        key (aes/decrypt secret (:key params))
        _  (println "KEY:" key)
        ci3  (-> (gh-file url key ref "ci3.yaml")
                 clj-yaml.core/parse-string)
        ;; cl   (io.kubernetes.client.util.Config/fromConfig "/tmp/cfg.yaml")
        ]

    {:status 200
     :body (cheshire.core/generate-string
            {:ci3 (dissoc ci3 :k8s)
             ;; :exec (exec cl "default" "ci3-0" ["ls" "-lah" "/data/inc"])
             :k8s (aes/decrypt secret (:k8s ci3))})}))

;; curl -X POST http://localhost:8668/enc --data-binary @k8s.yaml > enced

(defn encrypt [{secret :secret} req]
  (let [body (slurp (:body req))]
    {:status 200
     :header {"Content-Type" "text"}
     :body   (aes/encrypt secret body)}))

(defn decrypt [{secret :secret} req]
  (let [body (slurp (:body req))]
    {:status 200
     :header {"Content-Type" "text"}
     :body (aes/decrypt secret body)}))

(defn handle [ctx {uri :uri :as req}]
  (cond
    (= uri "/enc") (encrypt ctx req)
    (= uri "/dec") (decrypt ctx req)
    (and (= :get (:request-method req)) (= uri "/")) (hello ctx req)
    (= uri "/")    (hook ctx req)))

(defn mk-handler [ctx]
  (fn [req]
    (handle ctx req)))

(defn start [port]
  (let [secret (System/getenv "CI3_SECRET")]
     (when-not secret
        (throw (Exception. (str "CI3_SECRET is required. Here is new one generated for you - " (aes/gen-key)))))
     (org.httpkit.server/run-server (mk-handler {:secret secret}) {:port port})))

(comment
  (def srv (start 8668))
  (srv)


  )
