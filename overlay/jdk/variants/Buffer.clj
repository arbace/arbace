;; Go-build variant of java.nio.Buffer (C2G-SPEC §4.6; JRT-NOTES.md, "The JDK's resource data"):
;; read by c2g only. Buffer's static initializer gives jdk.internal.access.SharedSecrets the
;; package's JavaNioAccess (direct and mapped buffers, memory segments, the buffer pool: all
;; outside the Go build's world, as SharedSecrets' setter); the variant removes it, so that the
;; heap buffers initialize. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.nio)

(c2g/variant Buffer
  (c2g/cut (static-initializer 0)))
