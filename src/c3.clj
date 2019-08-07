(ns c3
  (:require [org.httpkit.server]
            [org.httpkit.client]
            [cheshire.core]
            [sodium]
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

(defn gh-file-req [url key commit file]
  {:query-params {:ref commit}
   :url (str url file)
   :headers {"Authorization" (str "token " key)
             "Accept" "application/vnd.github.v3.raw"}})

(defn hello [ctx req]
  {:status 300
   :body "
<html>

<body>
<center><h1>Welcome to C3</h1></center>
</body>


</html>"})

(defn parse-params [qs]
  (reduce (fn [acc p]
            (let [[k v] (str/split p #"=")]
              (assoc acc (keyword k) v)))
          {} (str/split qs #"&")))

(defn hook [{secret :secret {*http :http} :fx :as ctx} {params :params body :body :as req}]
  (let [body (cheshire.core/parse-string body keyword)
        url  (str/replace (get-in body [:repository :contents_url]) #"\{\+path\}$" "/")
        ref "master"
        key (sodium/decrypt secret (:key params))
        c3 (*http ctx (gh-file-req url- key ref "c3.yaml"))]

    {:status 200
     :body (cheshire.core/generate-string
            c3
            #_{
             :c3 c3
             :exec (exec cl "default" "c3-0" ["ls" "-lah" "/data/inc"])
             :k8s (sodium/decrypt secret (:k8s c3))
             })}))

;; curl -X POST http://localhost:8668/enc --data-binary @k8s.yaml > enced

(defn read-body [body]
  (when body
    (if (string? body)
      body
      (slurp body))))

(defn encrypt [{secret :secret} req]
  (let [body (read-body (:body req))]
    {:status 200
     :header {"Content-Type" "text"}
     :body   (sodium/encrypt secret body)}))

(defn decrypt [{secret :secret} req]
  (let [body (read-body (:body req))]
    {:status 200
     :header {"Content-Type" "text"}
     :body (sodium/decrypt secret body)}))

(defn handle [ctx {uri :uri qs :query-string body :body :as req}]
  (let [req (cond-> req
              qs (assoc :params (parse-params qs))
              body (assoc :body (read-body body)))]
    (cond
      (= uri "/enc") (encrypt ctx req)
      (= uri "/dec") (decrypt ctx req)
      (and (= :get (:request-method req)) (= uri "/")) (hello ctx req)
      (= uri "/")    (hook ctx req))))

(defn mk-handler [ctx]
  (fn [req]
    (handle ctx req)))

(defn get-secret []
  (if-let [secret (System/getenv "C3_SECRET")]
    secret
    (throw (Exception. (str "C3_SECRET is required. Here is new one generated for you - " (sodium/gen-key))))))

;; {:port 8668 :secret "abcd"}
(defn start [{:keys [port secret]}]
  (org.httpkit.server/run-server (mk-handler {:secret secret}) {:port port}))

(comment
  (def srv (start {:port 8668 :secret (get-secret)}))
  (srv)


  )
