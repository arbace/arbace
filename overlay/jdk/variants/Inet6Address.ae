;; Go-build variant of java.net.Inet6Address (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Sockets"):
;; read by c2g only. The native init (JNI's field ids) has no Go meaning. The constructor of a
;; name and an address does what initif does without a NetworkInterface (outside the closed
;; world; the JDK passes null there): the address, then the name and the family. The replacing methods keep jdk26u's code where they keep it (baf63fb; Copyright (c)
;; Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant Inet6Address
  (c2g/cut (static-initializer 0))
  (c2g/cut ^:private ^:static ^:native init ^void [])

  (constructor [this ^String hostName ^byte/1 addr]
    (set! holder6 (Inet6AddressHolder.))
    (.setAddr holder6 addr)
    (.init (.-holder this) hostName (if (== (alength addr) INADDRSZ) Inet6Address/IPv6 -1))))

;; the scope's interface is a NetworkInterface, a field the Go build drops: an address has none
;; (scope_ifname_set stays false), so its text is the address and the numeric scope
(c2g/variant Inet6Address$Inet6AddressHolder
  (method getHostAddress ^String [this]
    (let [^:mutable s (Inet6Address/numericToTextFormat ipaddress)]
      (when scope_id_set (set! s (java-str s "%" scope_id)))
      s)))
