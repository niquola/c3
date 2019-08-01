(ns aes
  (:import
   (java.nio.charset StandardCharsets)
   (java.security MessageDigest)
   (java.util Base64 Arrays)
   (javax.crypto Cipher)
   (javax.crypto KeyGenerator)
   (javax.crypto.spec SecretKeySpec IvParameterSpec))
  (:require [clojure.string :as str]))


(defn asci-bytes [x]
  (.getBytes x StandardCharsets/US_ASCII))

(defn base64decode [x]
  (let [dec (Base64/getDecoder)]
    (.decode dec x)))

(defn base64encode [x]
  (let [dec (Base64/getEncoder)]
    (String. (.encode dec x))))

(defn md5 [data]
  (let [md (MessageDigest/getInstance "MD5")]
    (.digest md data)))

(defn gen-iv []
  (String. 
   (Arrays/copyOfRange
    (.getBytes (str/replace (.toString (java.util.UUID/randomUUID)) #"-" ""))
    0 16)))

(defn encrypt [key value]
  (let [iv       (gen-iv)
        iv'      (IvParameterSpec. (.getBytes iv "UTF-8"))
        key      (base64decode key)
        skey     (SecretKeySpec. key "AES")
        cipher   (Cipher/getInstance "AES/CBC/PKCS5PADDING")
        _ (.init cipher Cipher/ENCRYPT_MODE, skey, iv')
        enc (.doFinal cipher (.getBytes value))]
    (str iv "_" (base64encode enc))))


(defn decrypt [key value]
  (let [[iv value] (str/split value #"_" 2)
        iv'      (IvParameterSpec. (.getBytes iv "UTF-8"))
        key      (base64decode key)
        skey     (SecretKeySpec. key "AES")
        cipher   (Cipher/getInstance "AES/CBC/PKCS5PADDING")
        _ (.init cipher Cipher/DECRYPT_MODE, skey, iv')
        dec (.doFinal cipher (base64decode value))]
    (String. dec)))

(defn gen-key [] 
  (let [g (KeyGenerator/getInstance "AES")]
    (.init g 256)
    (-> (.generateKey g)
        (.getEncoded)
        (base64encode))))


(comment
  (def ci
    (encrypt k "
pod: string
even:
 - more: 1
"))

  ci

  (decrypt k ci)


  )



;; (def ke "nl1fOSFJpnl9aM4urTMNobUIjC4/ikyUkx0miHrvqOI=")

;; (base64decode ke)


