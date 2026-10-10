;; Go-build variant of com.sun.crypto.provider.SunJCE (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Security"): read by c2g only. The SunJCE provider of the Go build registers the services it
;; implements, the HMACs of the MD5, SHA-1, SHA-2 and SHA-3 digests, with the JDK's aliases and
;; attributes; its ciphers, key agreements, key generators and factories, KEMs, KDFs, parameters,
;; key stores and PBE MACs are left out (their classes, and the engine classes Cipher,
;; KeyGenerator ..., are not in the Go build), so getInstance of them is the JDK's
;; NoSuchAlgorithmException. The replacing method keeps jdk26u's code where it keeps it
;; (baf63fb; Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception:
;; LICENSE.md); the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'com.sun.crypto.provider)

(c2g/variant SunJCE
  (method putEntries ^void [this]
    (let [attrs (HashMap. 3)]
      (.put attrs "SupportedKeyFormats" "RAW")
      (.ps this "Mac" "HmacMD5" "com.sun.crypto.provider.HmacMD5" nil attrs)
      (.psA this "Mac" "HmacSHA1" "com.sun.crypto.provider.HmacSHA1" attrs)
      (.psA this "Mac" "HmacSHA224" "com.sun.crypto.provider.HmacCore$HmacSHA224" attrs)
      (.psA this "Mac" "HmacSHA256" "com.sun.crypto.provider.HmacCore$HmacSHA256" attrs)
      (.psA this "Mac" "HmacSHA384" "com.sun.crypto.provider.HmacCore$HmacSHA384" attrs)
      (.psA this "Mac" "HmacSHA512" "com.sun.crypto.provider.HmacCore$HmacSHA512" attrs)
      (.psA this "Mac" "HmacSHA512/224" "com.sun.crypto.provider.HmacCore$HmacSHA512_224" attrs)
      (.psA this "Mac" "HmacSHA512/256" "com.sun.crypto.provider.HmacCore$HmacSHA512_256" attrs)
      (.psA this "Mac" "HmacSHA3-224" "com.sun.crypto.provider.HmacCore$HmacSHA3_224" attrs)
      (.psA this "Mac" "HmacSHA3-256" "com.sun.crypto.provider.HmacCore$HmacSHA3_256" attrs)
      (.psA this "Mac" "HmacSHA3-384" "com.sun.crypto.provider.HmacCore$HmacSHA3_384" attrs)
      (.psA this "Mac" "HmacSHA3-512" "com.sun.crypto.provider.HmacCore$HmacSHA3_512" attrs))))
