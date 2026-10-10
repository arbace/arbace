;; Go-build variant of java.security.Provider (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Security"):
;; read by c2g only.
;; - The static initializer describes the same engine types, but for the constructor parameter
;;   classes outside the Go build: Policy.Parameters (java.security.Policy) and
;;   javax.security.auth.login.Configuration.Parameters are null there, so the Policy and
;;   Configuration engines (whose classes are not in the Go build either) take no parameter.
;; - A service's implementation is made by jrt's sun.security.provider.JrtServices when it knows the class (the
;;   SUN and SunJCE services of the Go build: a direct constructor call, where the JDK finds the
;;   class by name and calls its constructor by reflection, which the closed world cannot do for
;;   the classes of sun.security.provider and com.sun.crypto.provider, the REPL's world leaving
;;   out non-exported packages, C2G-SPEC §10.6), else by reflection as the JDK does (a provider
;;   the evaluator defines). The reflective code is jdk26u's.
;; The replacing methods keep jdk26u's code where they keep it (baf63fb; Copyright (c) Oracle
;; and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest Copyright (c)
;; the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.security)

(c2g/variant Provider
  (static-initializer
    (set! knownEngines (java.util.HashMap.))
    (Provider/addEngine "AlgorithmParameterGenerator" false nil)
    (Provider/addEngine "AlgorithmParameters" false nil)
    (Provider/addEngine "KeyFactory" false nil)
    (Provider/addEngine "KeyPairGenerator" false nil)
    (Provider/addEngine "KeyStore" false nil)
    (Provider/addEngine "MessageDigest" false nil)
    (Provider/addEngine "SecureRandom" false SecureRandomParameters)
    (Provider/addEngine "Signature" true nil)
    (Provider/addEngine "CertificateFactory" false nil)
    (Provider/addEngine "CertPathBuilder" false nil)
    (Provider/addEngine "CertPathValidator" false nil)
    (Provider/addEngine "CertStore" false java.security.cert.CertStoreParameters)
    (Provider/addEngine "Cipher" true nil)
    (Provider/addEngine "ExemptionMechanism" false nil)
    (Provider/addEngine "Mac" true nil)
    (Provider/addEngine "KeyAgreement" true nil)
    (Provider/addEngine "KeyGenerator" false nil)
    (Provider/addEngine "SecretKeyFactory" false nil)
    (Provider/addEngine "KEM" true nil)
    (Provider/addEngine "KDF" false javax.crypto.KDFParameters)
    (Provider/addEngine "KeyManagerFactory" false nil)
    (Provider/addEngine "SSLContext" false nil)
    (Provider/addEngine "TrustManagerFactory" false nil)
    (Provider/addEngine "GssApiMechanism" false nil)
    (Provider/addEngine "SaslClientFactory" false nil)
    (Provider/addEngine "SaslServerFactory" false nil)
    ;; Policy.Parameters and Configuration.Parameters are not in the Go build
    (Provider/addEngine "Policy" false nil)
    (Provider/addEngine "Configuration" false nil)
    (Provider/addEngine "XMLSignatureFactory" false nil)
    (Provider/addEngine "KeyInfoFactory" false nil)
    (Provider/addEngine "TransformService" false nil)
    (Provider/addEngine "TerminalFactory" false Object)))

(c2g/variant Provider$Service
  (method ^:private newInstanceUtil :throws [Exception] ^Object [this ^{:tag (Class ?)} ctrParamClz
                                                                 ^Object ctorParamObj]
    (let [o (sun.security.provider.JrtServices/newInstance className ctrParamClz ctorParamObj)]
      (when (some? o)
        (return o)))
    (if (nil? ctrParamClz)
        (.newInstanceOf this)
        (try
          (let [^{:tag (Constructor ?)} con (.getConstructor (.getImplClass this) ctrParamClz)]
            (.newInstance con ctorParamObj))
          (catch NoSuchMethodException nsme
            (if (nil? ctorParamObj)
                (try
                  (.newInstanceOf this)
                  (catch NoSuchMethodException nsme2
                    (.addSuppressed nsme nsme2)
                    (throw nsme)))
                (throw nsme)))))))
