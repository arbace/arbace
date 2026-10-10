;; Go-build variant of java.net.URL (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"): read by c2g
;; only. URL is translated from jdk26u. The handlers are found as the JDK finds them without
;; installed providers (the ServiceLoader lookup, over ScopedValue, is outside the closed world:
;; none is found), and the default factory knows the handlers the Go build has: file: (with
;; its connections), http:, https: and jar: (their syntax; their connections are outside the
;; world). The static initializer that hands URL's handler to the JDK's other packages
;; (SharedSecrets' JavaNetURLAccess, jrt's lacks it) is gone, and so are the static fields of
;; the lookup's ScopedValue and of serialization (ObjectStreamField), outside the world.
;; (URLStreamHandler's hashCode and hostsEqual, which resolve the host through InetAddress, are
;; the JDK's since InetAddress is in the world: JRT-NOTES.md, "Sockets".)
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
