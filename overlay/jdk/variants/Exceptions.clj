;; Go-build variant of jdk.internal.util.Exceptions (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Sockets"): read by c2g only. Which host and user details exception messages may show comes
;; from jdk.includeInExceptions, a security property that a system property of the same name
;; overrides (sun.security.util.SecurityProperties); java.security's properties are not in the Go
;; build, so the variant reads the system property and defaults to JDK 26's java.security value,
;; hostInfoExclSocket. The replacing methods keep jdk26u's code where they keep it (baf63fb; Copyright (c)
;; Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'jdk.internal.util)

(c2g/variant Exceptions
  (method ^:public ^:static setup ^void []
    (when-not initialized
      (set! enhancedSocketExceptionText (Exceptions/included "hostInfo"))
      (set! enhancedNonSocketExceptionText
            (or (Exceptions/included "hostInfoExclSocket") enhancedSocketExceptionText))
      (set! enhancedUserExceptionText (Exceptions/included "userInfo"))
      (set! enhancedJarExceptionText (Exceptions/included "jar"))
      (set! initialized true)))

  (c2g/add
    (method ^:private ^:static included ^boolean [^String refName]
      (let [val (System/getProperty "jdk.includeInExceptions" "hostInfoExclSocket")]
        (loop [^int i 0 tokens (.split val ",")]
          (if (< i (alength tokens))
              (if (.equalsIgnoreCase (.trim (aget tokens i)) refName)
                  true
                  (recur (unchecked-inc-int i) tokens))
              false))))))
