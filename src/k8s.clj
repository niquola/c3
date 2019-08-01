(ns k8s
  (:require [cheshire.core]
            [clj-yaml.core]
            [org.httpkit.client]
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

(defn string->stream
  ([s] (string->stream s "UTF-8"))
  ([s encoding]
   (-> s
       (.getBytes encoding)
       (java.io.ByteArrayInputStream.))))

(defn ctx-for [cfg]
  (let [cl     (io.kubernetes.client.util.Config/fromConfig (string->stream cfg))
        token  (-> (.get (.getAuthentications cl) "BearerToken")
                (.getApiKey))
        base   (.getBasePath cl)]
    {:req-ops {:headers {"Authorization" (str "Bearer " token)}}
     :base base
     :client cl}))

(defn api-req [ctx opts]
  (let [resp @(org.httpkit.client/request
               (merge
                opts
                (:req-ops ctx)
                {:url  (str (:base ctx) (:url opts))
                 :headers (merge (:headers (:req-ops ctx))
                                 (:headers opts))}))]
    (cheshire.core/parse-string (:body resp) keyword)))

(defn api-get [ctx url]
  (api-req ctx {:url url}))

(defn api-delete [ctx url]
  (api-req ctx {:url url :method :delete}))

(defn api-post[ctx url res]
  (api-req ctx {:url url
                :method :post
                :headers {"Content-Type" "application/json"}
                :body (cheshire.core/generate-string res)}))

(defn exec [{cl :client :as ctx} ns pod cnt cmd]
  (let [exec (io.kubernetes.client.Exec. cl)
        proc (.exec exec ns pod (into-array cmd) cnt true true)
        out (-> proc
                .getInputStream
                java.io.InputStreamReader.
                java.io.BufferedReader.)]
    (loop []
      (when-let [ l (.readLine out)]
        (println l)
        (recur)))))



(comment

  (def ctx (ctx-for (slurp "keys/k8s.yaml")))

  (spit "/tmp/c3.yaml"
        (clj-yaml.core/generate-string c3))

  (def c3
    {:k8s (slurp "keys/k8s.yaml")
     :job {:apiVersion "v1"
           :kind "Pod"
           :metadata {:name "buildit"
                      :labels {:system "c3"}}
           :spec {:restartPolicy "Never"
                  :volumes [{:name "docker-sock" :hostPath {:path "/var/run/docker.sock"}}
                            {:name "project" :emptyDir {}}]
                  :containers
                  [{:name "main"
                    :image "ubuntu" 
                    :imagePullPolicy "Always"
                    :command ["sleep"]
                    :args ["100000000"]
                    :volumeMounts [{:name "docker-sock" :mountPath "/var/run/docker.sock"}
                                   {:name "project"     :mountPath "/ci3"}]}
                   {:name "git"
                    :image "alpine/git"
                    :imagePullPolicy "Always"
                    :command ["sleep"]
                    :args ["100000000"]
                    :volumeMounts [{:name "project"     :mountPath "/ci3"}]}

                   {:name "clj"
                    :image "clojure:openjdk-11-tools-deps"
                    :imagePullPolicy "Always"
                    :command ["sleep"]
                    :args ["100000000"]
                    :volumeMounts [{:name "project"     :mountPath "/ci3"}]}

                   {:name "kaniko"
                    :image "cohalz/kaniko-alpine"
                    :imagePullPolicy "Always"
                    :command ["sleep"]
                    :args ["10000000"]
                    :volumeMounts [{:name "project" :mountPath "/ci3"}]}

                   {:name "kube"
                    :image "wernight/kubectl"
                    :imagePullPolicy "Always"
                    :command ["sleep"]
                    :args ["1000000000"]
                    :volumeMounts [{:name "project"     :mountPath "/ci3"}]}

                   ]}}})

  ctx

  (api-get ctx "/api/v1/namespaces/default/pods")

  (api-get ctx "/api/v1/namespaces/default/pods/buildit")

  (api-post ctx "/api/v1/namespaces/default/pods" (:job c3))

  (api-delete ctx "/api/v1/namespaces/default/pods/buildit")

  (exec ctx "default" "buildit" "main" ["bash" "-c" "ls -lah /ci3"])

  (exec ctx "default" "buildit" "git"
        ["sh" "-c" "git clone https://<token>@github.com/HealthSamurai/inc.git /ci3/project "])

  (exec ctx "default" "buildit" "git"
        ["sh" "-c" "
ls -lah /ci3/project
"])

  (exec ctx "default" "buildit" "kaniko"
        ["sh" "-c" "
/kaniko/executor \\
--dockerfile=/ci3/project/Dockerfile \\
--context=/ci3/project \\
--no-push \\
--tarPath=/ci3/img
"])

  (exec ctx "default" "buildit" "clj" ["sh" "-c" "cd /ci3/project && clojure -A:build"])

  (exec ctx "default" "buildit" "kube" ["sh" "-c" "kubectl get pods"])



  )
