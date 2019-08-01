# c3

A ultra-minimalistic ci/cd for k8s

## Motivataion

k8s provides us with nice platform to run isolated payloads.
c3 is hacker's ci which could provide CI/CD inside k8s cluster

## Concept

c3 is stateless service, which can serve multiple ci/cd

Flow:

1. get access token for repo
2. encrypt with c3 /encrpyt endpoint
3. setup webhook with encrypted access key as key parameter in query-string
4. on webhook c3 using github api read c3.yaml file from repo for this revision
5. c3.yaml contains k8s key with encrypted by c3 config for k8s
6. c3 read k8s config and job description, which is k8s pod
7. c3 run this pod or reuse existing for this project
8. using k8s exec c3 run build commands in pod containers and report results into telegram

```yaml

k8s: <encrypted-k8s-config>
# pod definition
job:
  apiVersion: v1
  kind: Pod
  metadata:
    name: buildit
    labels: {system: c3}
  spec:
    restartPolicy: Never
    volumes:
    - name: docker-sock
      hostPath: {path: /var/run/docker.sock}
    - name: project
      emptyDir: {}
    containers:
    - name: main
      image: ubuntu
      imagePullPolicy: Always
      command: [sleep]
      args: ['100000000']
      volumeMounts:
      - {name: docker-sock, mountPath: /var/run/docker.sock}
      - {name: project, mountPath: /c3}
    - name: git
      image: alpine/git
      imagePullPolicy: Always
      command: [sleep]
      args: ['100000000']
      volumeMounts:
      - {name: project, mountPath: /c3}
    - name: clj
      image: clojure:openjdk-11-tools-deps
      imagePullPolicy: Always
      command: [sleep]
      args: ['100000000']
      volumeMounts:
      - {name: project, mountPath: /c3}
    - name: kaniko
      image: cohalz/kaniko-alpine
      imagePullPolicy: Always
      command: [sleep]
      args: ['10000000']
      volumeMounts:
      - {name: project, mountPath: /c3}
    - name: kube
      image: wernight/kubectl
      imagePullPolicy: Always
      command: [sleep]
      args: ['1000000000']
      volumeMounts:
      - {name: project, mountPath: /c3}

steps:
- git: git submodule init && git submodule update
- clj: clojure -A:test
- clj: clojure -A:jar
- kaniko:  |
  /kaniko/executor \
  --dockerfile=/c3/project/Dockerfile \
  --context=/c3/project \
  --no-push \
  --tarPath=/c3/img


```



Copyright © 2019 niquola aitem

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

---
