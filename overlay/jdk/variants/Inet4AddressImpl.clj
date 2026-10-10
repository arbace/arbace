;; Go-build variant of java.net.Inet4AddressImpl (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Sockets"): read by c2g only. Its natives are the host's, as Inet6AddressImpl's
;; (Inet6AddressImpl.clj), for IPv4 alone. The replacing methods keep jdk26u's code where they keep it (baf63fb; Copyright (c)
;; Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant Inet4AddressImpl
  (method ^:public getLocalHostName :throws [UnknownHostException] ^String [this]
    (jdk.internal.jrt.HostNet/getLocalHostName))

  (method ^:private lookupAllHostAddr :throws [UnknownHostException] ^InetAddress/1 [this ^String hostname]
    (jdk.internal.jrt.HostNet/lookupAllHostAddr hostname java.net.spi.InetAddressResolver$LookupPolicy/IPV4))

  (method ^:public getHostByAddr :throws [UnknownHostException] ^String [this ^byte/1 addr]
    (jdk.internal.jrt.HostNet/getHostByAddr addr))

  (method ^:private isReachable0 :throws [java.io.IOException] ^boolean [this ^byte/1 addr ^int timeout
                                                                 ^byte/1 ifaddr ^int ttl]
    (throw (UnsupportedOperationException. "InetAddress.isReachable is not in the Go build"))))
