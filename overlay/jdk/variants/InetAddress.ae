;; Go-build variant of java.net.InetAddress (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Sockets"): read
;; by c2g only.
;; - The first static initializer reads the same properties, and leaves out what has no Go
;;   meaning: the native library, SharedSecrets' access (none of its users is in the Go build),
;;   the native init. The second asks the host whether it has IPv6 (Inet6AddressImpl, whose
;;   natives are the host's: Inet6AddressImpl.clj) and IPv4.
;; - The resolver is the built-in one: no InetAddressResolverProvider is loaded (ServiceLoader is
;;   not in the Go build).
;; - The lookup cache is a map from host to CachedLookup, which expires as the JVM's default
;;   policy does without a security manager (sun.net.InetAddressCachePolicy: 30 s for addresses,
;;   10 s for failures; java.security's networkaddress.cache.* properties are not in the Go
;;   build). The JVM's ConcurrentSkipListSet of expiries (outside the closed world) and its one
;;   lookup at a time per host (NameServiceAddresses) are left out: an entry is checked when it
;;   is used, and concurrent first lookups of a host each ask the name service.
;; - PlatformResolver calls the name service without Blocker (virtual threads' compensation).
;; The replacing methods keep jdk26u's code where they keep it (baf63fb; Copyright (c)
;; Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant InetAddress
  ^{:c2g/nth 0}
  (static-initializer
    (set! PREFER_IPV4_STACK_VALUE (System/getProperty "java.net.preferIPv4Stack"))
    (set! PREFER_IPV6_ADDRESSES_VALUE (System/getProperty "java.net.preferIPv6Addresses"))
    (set! HOSTS_FILE_NAME (System/getProperty "jdk.net.hosts.file")))

  (c2g/cut ^:private ^:static ^:native init ^void [])

  (method ^:private ^:static isIPv4Available ^boolean []
    (jdk.internal.jrt.HostNet/hasFamily0 false))

  (method ^:private ^:static isIPv6Supported ^boolean []
    (jdk.internal.jrt.HostNet/hasFamily0 true))

  (method ^:private ^:static resolver ^InetAddressResolver []
    BUILTIN_RESOLVER)

  (c2g/cut (field ^:private ^:static ^:final ^{:tag (NavigableSet CachedLookup)} expirySet))

  (method ^:private ^:static getAllByName0 :throws [UnknownHostException] ^InetAddress/1 [^String host
                                                                                          ^boolean useCache]
    (let [now (System/nanoTime)
          c (when useCache (.get cache host))]
      (if (and (instance? InetAddress$CachedLookup c)
               (>= (unchecked-subtract (.-expiryTime ^InetAddress$CachedLookup c) now) 0))
          (.clone (.get ^InetAddress$CachedLookup c))
          (let [^:mutable ^InetAddress/1 addrs nil
                ^:mutable ^UnknownHostException ex nil]
            (try
              (set! addrs (InetAddress/getAddressesFromNameService host))
              (catch UnknownHostException e (set! ex e)))
            (.put cache host (InetAddress$CachedLookup. host addrs
                                                        (unchecked-add now (if (nil? ex) 30000000000 10000000000))))
            (when (some? ex) (throw ex))
            (.clone addrs))))))

(c2g/variant InetAddress$CachedLookup
  (method ^:public tryRemoveExpiredAddress ^boolean [this ^long now]
    (when (< (unchecked-subtract expiryTime now) 0)
      (.remove InetAddress/cache host this)
      (return true))
    false))

(c2g/variant InetAddress$ValidCachedLookup
  (method ^:public tryRemoveExpiredAddress ^boolean [this ^long now]
    false))

(c2g/variant InetAddress$PlatformResolver
  (method ^:public lookupByName :throws [UnknownHostException] ^{:tag (Stream InetAddress)} [this
                                                                                             ^String host
                                                                                             ^InetAddressResolver$LookupPolicy policy]
    (java.util.Objects/requireNonNull host)
    (java.util.Objects/requireNonNull policy)
    (InetAddress/validate host)
    (java.util.Arrays/stream (.lookupAllHostAddr InetAddress/impl host policy)))

  (method ^:public lookupByAddress :throws [UnknownHostException] ^String [this ^byte/1 addr]
    (java.util.Objects/requireNonNull addr)
    (when (and (not (== (alength addr) Inet4Address/INADDRSZ))
               (not (== (alength addr) Inet6Address/INADDRSZ)))
      (throw (IllegalArgumentException. "Invalid address length")))
    (.getHostByAddr InetAddress/impl addr)))

;; not made by the variant's getAllByName0: the lookup itself, without the cache's bookkeeping
(c2g/variant InetAddress$NameServiceAddresses
  (method ^:public get :throws [UnknownHostException] ^InetAddress/1 [this]
    (InetAddress/getAddressesFromNameService host)))
