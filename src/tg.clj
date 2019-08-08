(ns tg
  (:require [org.httpkit.client :as http]
            [cheshire.core :as cheshire]))

(def base "https://api.telegram.org/bot")

(defn send-message
  ([token chat-id message-id text]
   (let [url (str base token (if message-id "/editMessageText" "/sendMessage"))
         body {:chat_id chat-id :text text :message_id message-id :parse_mode "Markdown"}]
     @(http/post url {:headers {"Content-Type" "application/json"}
                      :body (cheshire/generate-string body)
                      :form-params  body}))))

(comment
  (def chat-id (read-line))
  (def token (read-line))
  (defn s [message-id text]
    (send-message token chat-id message-id text))

  (def resp (s nil "test from REPL"))
  (def body (cheshire/parse-string (:body resp) keyword))
  (def message-id (-> body :result :message_id))

  (s message-id "updated text")

  (require 'report)

  (s message-id (report/report {:repo-full-name "dottedmag/c3"
                                :commit "ea7f20b8672acaba97ff1e73d19856372b8109aa"
                                :steps [{:name "beat off the meat" :state :succeeded}
                                        {:name "bread the meat" :state :succeeded}
                                        {:name "fry the meat" :state :succeeded}]})))
