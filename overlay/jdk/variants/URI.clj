;; Go-build variant of java.net.URI (C2G-SPEC §4.6): read by c2g only. URI's static initializer
;; registers a JavaNetUriAccess with SharedSecrets for the JDK's internals (the module system,
;; jar URLs), none of which is in the Go build; the variant registers nothing. Copyright (c) the
;; Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant URI
  (c2g/cut (static-initializer 0)))
