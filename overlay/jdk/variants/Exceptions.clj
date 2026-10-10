;; Go-build variant of jdk.internal.util.Exceptions (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Files"): read by c2g only. Exceptions is translated from jdk26u (URL's and the address
;; checks' messages); setup reads the security property jdk.includeInExceptions through
;; SecurityProperties, outside the closed world: the variant takes the JDK's default,
;; hostInfoExclSocket (conf/security/java.security), so the non-socket host information is in
;; the messages and the rest is not. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'jdk.internal.util)

(c2g/variant Exceptions
  (method ^:public ^:static setup ^void []
    (when-not initialized
      (set! enhancedSocketExceptionText false)
      (set! enhancedNonSocketExceptionText true)
      (set! enhancedUserExceptionText false)
      (set! enhancedJarExceptionText false)
      (set! initialized true))))
