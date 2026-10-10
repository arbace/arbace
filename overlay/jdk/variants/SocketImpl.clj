;; Go-build variant of java.net.SocketImpl (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Sockets"): read
;; by c2g only. The platform's SocketImpl is jrt's own jdk.internal.jrt.HostSocketImpl
;; (overlay/jdk), over the host's sockets, in place of sun.nio.ch.NioSocketImpl (java.nio's
;; channels are outside the closed world). Copyright (c) the Arbace authors; Eclipse Public
;; License 1.0.
(in-ns 'java.net)

(c2g/variant SocketImpl
  (method ^:static createPlatformSocketImpl :type-params [(S extends SocketImpl sun.net.PlatformSocketImpl)] ^S [^boolean server]
    ^SocketImpl (jdk.internal.jrt.HostSocketImpl. server)))
