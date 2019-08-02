(ns sodium
  (:import
   (com.goterl.lazycode.lazysodium SodiumJava LazySodiumJava)
   (com.goterl.lazycode.lazysodium.interfaces SecretBox)
   (com.goterl.lazycode.lazysodium.utils Key)
   (com.goterl.lazycode.lazysodium.exceptions SodiumException)))

(def ^:private lib
  (delay (LazySodiumJava. (SodiumJava.))))

(defn gen-key []
  (.getAsHexString (.cryptoSecretBoxKeygen @lib)))

(defn encrypt [key plaintext]
  (let [nonce (.nonce @lib SecretBox/NONCEBYTES)
        ciphertext (.cryptoSecretBoxEasy @lib
                                         plaintext
                                         nonce
                                         (Key/fromHexString key))]
    (when ciphertext
      (str (.toHexStr @lib nonce) "." ciphertext))))

(def ^:private ciphertext-re #"([0-9A-F]{48})\.([0-9A-F]+)")

(defn decrypt [key ciphertext]
  (when-let [[_ nonce ciphertext] (re-matches ciphertext-re  ciphertext)]
    (try
     (.cryptoSecretBoxOpenEasy @lib
                               ciphertext
                               (.toBinary @lib nonce)
                               (Key/fromHexString key))
     (catch SodiumException e))))

(comment
  (def k (gen-key))
  (encrypt k "")
  (def ct (encrypt k "hello"))
  (decrypt k ct)
  (decrypt k "bullshit")
  ;; Wrong ciphertext
  (decrypt k "68BC15939981BF8F26EF69E076DBC39DE13370274B4AFEC3.132966F8074CDD63C0869A91B3EADFFBBF7A59B874"))
