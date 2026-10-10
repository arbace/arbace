;; Go-build variant of java.net.URL (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"): read by c2g
;; only. URL is translated from jdk26u. The handlers are found as the JDK finds them without
;; installed providers (the ServiceLoader lookup, over ScopedValue, is outside the closed world:
;; none is found), and the default factory knows the handlers the Go build has: file: (with
;; its connections), http:, https: and jar: (their syntax; their connections are outside the
;; world). The static initializer that hands URL's handler to the JDK's other packages
;; (SharedSecrets' JavaNetURLAccess, jrt's lacks it) is gone, and so are the static fields of
;; the lookup's ScopedValue and of serialization (ObjectStreamField), outside the world.
;; URLStreamHandler's hashCode and hostsEqual compare host names: the host's address
;; (InetAddress, a name lookup) is outside the world, so they answer as the JDK does for a host
;; that does not resolve (for file: URLs, whose host is empty, exactly as the JDK).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant URL
  (c2g/cut (static-initializer 0))
  (c2g/cut (field ^:private ^:static ^:final ^{:tag (ScopedValue Boolean)} IN_LOOKUP))
  (c2g/cut (field ^:private ^:static ^:final ^ObjectStreamField/1 serialPersistentFields))
  (method ^:private ^:static lookupViaProviders ^URLStreamHandler [^:final ^String protocol]
    nil))

(c2g/variant URL$DefaultFactory
  (method ^:public createURLStreamHandler ^URLStreamHandler [this ^String protocol]
    (switch protocol
      "file" (return (sun.net.www.protocol.file.Handler.))
      "jar" (return (sun.net.www.protocol.jar.Handler.))
      "http" (return (sun.net.www.protocol.http.Handler.))
      "https" (return (sun.net.www.protocol.https.Handler.)))
    nil))

(c2g/variant URLStreamHandler
  (method ^:protected hashCode ^int [this ^URL u]
    (let [^:mutable ^int h 0
          protocol (.getProtocol u)]
      (when (some? protocol) (set! h (unchecked-add-int h (.hashCode protocol))))
      (let [host (.getHost u)]
        (when (some? host)
          (set! h (unchecked-add-int h (.hashCode (.toLowerCase host java.util.Locale/ROOT)))))
        (let [file (.getFile u)]
          (when (some? file) (set! h (unchecked-add-int h (.hashCode file))))
          (if (== (.getPort u) -1)
              (set! h (unchecked-add-int h (.getDefaultPort this)))
              (set! h (unchecked-add-int h (.getPort u))))
          (let [ref (.getRef u)]
            (when (some? ref) (set! h (unchecked-add-int h (.hashCode ref))))
            h)))))

  (method ^:protected hostsEqual ^boolean [this ^URL u1 ^URL u2]
    (cond
      (and (some? (.getHost u1)) (some? (.getHost u2)))
        (.equalsIgnoreCase (.getHost u1) (.getHost u2))
      :else (and (nil? (.getHost u1)) (nil? (.getHost u2))))))
