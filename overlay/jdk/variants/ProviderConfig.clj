;; Go-build variant of sun.security.jca.ProviderConfig (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Security"): read by c2g only. The Go build has two of the JDK's providers, SUN and SunJCE
;; (java.base's, in plain Java); the others of java.security's list (SunRsaSign, SunEC, SunJSSE,
;; SunJGSS, SunSASL, XMLDSig, SunPCSC, JdkLDAP, JdkSASL, SunPKCS11) are not in it, and a provider
;; named by its class is not loaded (no ServiceLoader, no class loading by name): those configs
;; fail to load, as a provider the JDK cannot find, so Security.getProviders() lists SUN and
;; SunJCE. The replacing method keeps jdk26u's code where it keeps it (baf63fb; Copyright (c)
;; Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.security.jca)

(c2g/variant ProviderConfig
  (method getProvider ^Provider [this]
    (let [^:mutable ^Provider p provider]
      (when (some? p)
        (return p))
      (locking this
        (set! p provider)
        (when (some? p)
          (return p))
        (when-not (.shouldLoad this)
          (return nil))
        (set! p (switch provName
                  ("SUN" "sun.security.provider.Sun") (sun.security.provider.Sun.)
                  ("SunJCE" "com.sun.crypto.provider.SunJCE") (com.sun.crypto.provider.SunJCE.)
                  (do (.disableLoad this) nil)))
        (set! provider p))
      p)))
