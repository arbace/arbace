;; Go-build variant of sun.security.provider.SunEntries (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Security"): read by c2g only. The SUN provider of the Go build registers the services it
;; implements: the SecureRandom generators (NativePRNG and its blocking and non-blocking kinds,
;; over the host's random source, DRBG, SHA1PRNG) and the message digests (MD2, MD5, SHA-1, the
;; SHA-2 and SHA-3 families, SHAKE128-256, SHAKE256-512), with the JDK's aliases and attributes,
;; in the JDK's order. Its signatures, key pair generators and factories, parameter generators,
;; certificate factory, key stores, cert stores, login configuration and cert path builders and
;; validators (DSA, HSS/LMS, ML-DSA, X.509, PKCS12, JKS, DKS, PKIX) are left out: their classes
;; (and the engine classes of their types: Signature, KeyStore, CertificateFactory ...) are not
;; in the Go build, so Provider.getService answers null for them and getInstance the JDK's
;; NoSuchAlgorithmException. The replacing constructor keeps jdk26u's code where it keeps it
;; (baf63fb; Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception:
;; LICENSE.md); the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.security.provider)

(c2g/variant SunEntries
  (constructor [this ^Provider p]
    (set! services (LinkedHashSet. 50 (float 0.9)))
    (let [attrs (HashMap. 3)]
      ;; SecureRandom engines
      (.put attrs "ThreadSafe" "true")
      (when (NativePRNG/isAvailable)
        (.add this p "SecureRandom" "NativePRNG" "sun.security.provider.NativePRNG" attrs))
      (when (NativePRNG$Blocking/isAvailable)
        (.add this p "SecureRandom" "NativePRNGBlocking" "sun.security.provider.NativePRNG$Blocking" attrs))
      (when (NativePRNG$NonBlocking/isAvailable)
        (.add this p "SecureRandom" "NativePRNGNonBlocking" "sun.security.provider.NativePRNG$NonBlocking" attrs))
      (.put attrs "ImplementedIn" "Software")
      (.add this p "SecureRandom" "DRBG" "sun.security.provider.DRBG" attrs)
      (.add this p "SecureRandom" "SHA1PRNG" "sun.security.provider.SecureRandom" attrs)
      ;; Digest engines (attrs as the JDK has them there: ImplementedIn)
      (.clear attrs)
      (.put attrs "ImplementedIn" "Software")
      (.addWithAlias this p "MessageDigest" "MD2" "sun.security.provider.MD2" attrs)
      (.addWithAlias this p "MessageDigest" "MD5" "sun.security.provider.MD5" attrs)
      (.addWithAlias this p "MessageDigest" "SHA-1" "sun.security.provider.SHA" attrs)
      (.addWithAlias this p "MessageDigest" "SHA-224" "sun.security.provider.SHA2$SHA224" attrs)
      (.addWithAlias this p "MessageDigest" "SHA-256" "sun.security.provider.SHA2$SHA256" attrs)
      (.addWithAlias this p "MessageDigest" "SHA-384" "sun.security.provider.SHA5$SHA384" attrs)
      (.addWithAlias this p "MessageDigest" "SHA-512" "sun.security.provider.SHA5$SHA512" attrs)
      (.addWithAlias this p "MessageDigest" "SHA-512/224" "sun.security.provider.SHA5$SHA512_224" attrs)
      (.addWithAlias this p "MessageDigest" "SHA-512/256" "sun.security.provider.SHA5$SHA512_256" attrs)
      (.addWithAlias this p "MessageDigest" "SHA3-224" "sun.security.provider.SHA3$SHA224" attrs)
      (.addWithAlias this p "MessageDigest" "SHA3-256" "sun.security.provider.SHA3$SHA256" attrs)
      (.addWithAlias this p "MessageDigest" "SHA3-384" "sun.security.provider.SHA3$SHA384" attrs)
      (.addWithAlias this p "MessageDigest" "SHA3-512" "sun.security.provider.SHA3$SHA512" attrs)
      (.addWithAlias this p "MessageDigest" "SHAKE128-256" "sun.security.provider.SHA3$SHAKE128Hash" attrs)
      (.addWithAlias this p "MessageDigest" "SHAKE256-512" "sun.security.provider.SHA3$SHAKE256Hash" attrs))))
