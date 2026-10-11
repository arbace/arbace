;; Go-build variant of java.util.random.RandomGenerator (C2G-SPEC §4.6; doc/go/JAVA-BASE.md,
;; amendment JB2): read by c2g only. isDeprecated asks whether the class has @Deprecated, a class
;; outside the closed world; the Go build keeps no annotations (C2G-SPEC §12, amendment R16:
;; isAnnotationPresent is false), so the variant answers false, which is what the JVM answers for
;; every generator the Go build has. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.random)

(c2g/variant RandomGenerator
  (method ^:default isDeprecated ^boolean [this] false))
