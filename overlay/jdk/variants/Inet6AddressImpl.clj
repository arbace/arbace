;; Go-build variant of java.net.Inet6AddressImpl (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Sockets"): read by c2g only. The platform's name service, JNI over getaddrinfo in the JDK, is
;; the host's (jrt's jdk.internal.jrt.HostNet, Go's resolver in B1a); isReachable (ICMP echo or a
;; TCP connection to port 7) is not in the Go build; the loopback address is the preferred
;; family's without asking NetworkInterface (outside the closed world) whether it is bound.
;; The replacing methods keep jdk26u's code where they keep it (baf63fb; Copyright (c)
;; Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant Inet6AddressImpl
  (method ^:public getLocalHostName :throws [UnknownHostException] ^String [this]
    (jdk.internal.jrt.HostNet/getLocalHostName))

  (method ^:private lookupAllHostAddr :throws [UnknownHostException] ^InetAddress/1 [this ^String hostname
                                                                                     ^int characteristics]
    (jdk.internal.jrt.HostNet/lookupAllHostAddr hostname characteristics))

  (method ^:public getHostByAddr :throws [UnknownHostException] ^String [this ^byte/1 addr]
    (jdk.internal.jrt.HostNet/getHostByAddr addr))

  (method ^:private isReachable0 :throws [java.io.IOException] ^boolean [this ^byte/1 addr ^int scope ^int timeout
                                                                 ^byte/1 inf ^int ttl ^int if_scope]
    (throw (UnsupportedOperationException. "InetAddress.isReachable is not in the Go build")))

  (method ^:public ^:synchronized loopbackAddress ^InetAddress [this]
    (when (nil? loopbackAddress)
      (let [flags (.characteristics InetAddress/PLATFORM_LOOKUP_POLICY)]
        (set! loopbackAddress
              (if (or (InetAddress/ipv6AddressesFirst flags) (InetAddress/systemAddressesOrder flags))
                  (Inet6Address. "localhost" (new byte/1 [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1]))
                  (Inet4Address. "localhost" (new byte/1 [0x7f 0x00 0x00 0x01]))))))
    loopbackAddress))
